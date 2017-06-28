#!/usr/bin/python
# -*- coding: utf-8 -*-
# Author: Cemre ALPSOY <cemre.alpsoy@agem.com.tr>

from base.plugin.abstract_plugin import AbstractPlugin
import json


class ServiceManagement(AbstractPlugin):
    def __init__(self, data, context):
        super(AbstractPlugin, self).__init__()
        self.data = data
        self.context = context
        self.logger = self.get_logger()
        self.message_code = self.get_message_code()
        self.service_status = 'service {} status'

    def start_stop_service(self, service_name, service_action):
        (result_code, p_out, p_err) = self.execute('service {0} {1}'.format(service_name, service_action))
        if result_code == 0:
            message = 'Service start/stop action was successful: '.format(service_action)

        else:
            message = 'Service action was unsuccessful: {0}, return code {1}'.format(service_action, str(result_code))

        self.logger.debug(message)
        return result_code, message

    def is_service_exist(self,service_name):
        result_code, p_out, p_err = self.execute("service --status-all")
        p_err = ' ' + p_err
        p_out = ' ' + p_out
        lines = p_out.split('\n')
        for line in lines:
            line_split = line.split(' ')
            if len(line_split) >= 5:
                result, out, err = self.execute(self.service_status.format(service_name))

                if 'Unknown job' not in str(err):
                    if line_split[len(line_split) - 4] == '+':
                        return "ACTIVE"
                    elif line_split[len(line_split) - 4] == '-':
                        return "INACTIVE"
                else:
                    return "NOTFOUND"

    def is_service_running(self, service_name):
        result_code, p_out, p_err = self.execute("ps -A")
        if service_name in p_out:
            return True
        else:
            return False

    def set_startup_service(self, service_name):
        (result_code, p_out, p_err) = self.execute('update-rc.d {} defaults'.format(service_name))

        if result_code == 0:
            message = 'Service startup action was successful: {}'.format(service_name)
        else:
            message = 'Service action was unsuccessful: {0}, return code {1}'.format(service_name, str(result_code))

        self.logger.debug('SERVICE' + message)
        return result_code, message

    def send_mail(self, stopped_services, all_services):
        if self.context.is_mail_send():
            mail_content = self.context.get_mail_content();
            if mail_content.__contains__('{stopped_services}'):
                mail_content = str(mail_content).replace('{stopped_services}', str(stopped_services));
            if mail_content.__contains__('{ahenk}'):
                mail_content = str(mail_content).replace('{ahenk}', str(self.Ahenk.dn()));

            self.context.set_mail_content(mail_content)

            self.context.create_response(code=self.message_code.TASK_PROCESSED.value,
                                         message='Servis izleme görevi başarıyla oluşturuldu.',
                                         data=json.dumps({
                                             'Result': 'İşlem Başarı ile gercekleştirildi',
                                             'mail_content': str(self.context.get_mail_content()),
                                             'mail_subject': str(self.context.get_mail_subject()),
                                             'mail_send': self.context.is_mail_send(),
                                             'services' :all_services
                                         }),
                                         content_type=self.get_content_type().APPLICATION_JSON.value)
        else:
            self.context.create_response(code=self.message_code.TASK_PROCESSED.value,
                                         message='Servis izleme görevi başarıyla oluşturuldu.',
                                         data=json.dumps({
                                             'Result': 'İşlem Başarı ile gercekleştirildi',
                                             'mail_send': self.context.is_mail_send(),
                                             'services': all_services
                                         }),
                                         content_type=self.get_content_type().APPLICATION_JSON.value)


    def handle_task(self):
        try:
            self.logger.debug("Service Management task is started.")
            services = self.data['serviceRequestParameters']
            stopped_services = ''

            result_code, p_out, p_err = self.execute("service --status-all")

            p_err = ' ' + p_err
            p_out = ' ' + p_out
            lines = p_out.split('\n')

            for service in services:
                service_name = service["serviceName"]
                serviceStatus = service["serviceStatus"]
                is_service_deleted= service["deleted"]

                #service["serviceStatus"]= "exist" if self.is_service_exist(service_name) else "not_exist"

                if (service_name in p_out) is False:
                    service["serviceStatus"] = 'NOTFOUND'
                else :
                    for line in lines:
                        line_split = line.split(' ')

                        if len(line_split) >= 5:
                            name = line_split[len(line_split) - 1]
                            if service_name == name :
                                if  line_split[len(line_split) - 4] == '+':
                                    service["serviceStatus"] = "ACTIVE"
                                elif line_split[len(line_split) - 4] == '-':
                                    service["serviceStatus"] = 'INACTIVE'

                    if service["serviceStatus"] == 'INACTIVE' and is_service_deleted is False:
                        stopped_services += service_name + " ,";

            if stopped_services != '':
                self.send_mail(stopped_services,services)
            else :
                self.context.create_response(code=self.message_code.TASK_PROCESSED.value,
                                             message='Servis izleme görevi başarıyla oluşturuldu.',
                                             data=json.dumps({
                                                 'Result': 'İşlem Başarı ile gercekleştirildi',
                                                 'services': services
                                             }),
                                             content_type=self.get_content_type().APPLICATION_JSON.value)

            # service_name = str((self.data)['serviceName'])
            # service_status = str((self.data)['serviceStatus'])
            # start_auto = bool((self.data)['startAuto'])
            # if service_status == 'Start' or service_status == 'Başlat':
            #     service_action = 'start'
            # else:
            #     service_action = 'stop'
            # result_code, message = self.start_stop_service(service_name, service_action)
            # if result_code == 0 and start_auto is True:
            #     result_code, message = self.set_startup_service(service_name)
            # if result_code == 0:
            #     self.context.create_response(code=self.message_code.TASK_PROCESSED.value,
            #                                  message='Servis başlatma/durdurma/otomatik başlatma işlemi başarıyla gerçekleştirildi',
            #                                  data=json.dumps(self.data),
            #                                  content_type=self.get_content_type().APPLICATION_JSON.value)
            # else:
            #     self.context.create_response(code=self.message_code.TASK_ERROR.value, message=message)

        except Exception as e:
            self.logger.error(str(e))
            self.context.create_response(code=self.message_code.TASK_ERROR.value,
                                         message='Servis yönetimi sırasında bir hata oluştu: {0}'.format(
                                             str(e)))


def handle_task(task, context):
    plugin = ServiceManagement(task, context)
    plugin.handle_task()
