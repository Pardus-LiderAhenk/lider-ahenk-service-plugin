#!/usr/bin/python
# -*- coding: utf-8 -*-
# Author: Cemre ALPSOY <cemre.alpsoy@agem.com.tr>

import json
import subprocess
from base.model.enum.ContentType import ContentType
from base.plugin.abstract_plugin import AbstractPlugin


class ServiceList(object):
    def __init__(self):
        self.service_list = []


class ServiceListItem:
    def __init__(self, service_name, status, auto):
        self.serviceName = service_name
        self.serviceStatus = status
        self.startAuto = auto


def encode_service_object(obj):
    if isinstance(obj, ServiceListItem):
        return obj.__dict__
    return obj


class GetServices(AbstractPlugin):
    def __init__(self, data, context):
        super(AbstractPlugin, self).__init__()
        self.data = data
        self.context = context
        self.logger = self.get_logger()
        self.message_code = self.get_message_code()

        self.temp_file_name = str(self.generate_uuid())
        self.file_path = '{0}{1}'.format(str(self.Ahenk.received_dir_path()), self.temp_file_name)

    def handle_task(self):
        try:
            self.logger.debug('[SERVICE] Executing command for service list.')
            service_list = self.get_service_status()


            self.logger.debug('[SERVICE] Command executed.')

            self.context.create_response(code=self.message_code.TASK_PROCESSED.value,
                                             message='Servis listesi başarıyla okundu.', data=service_list, content_type=ContentType.APPLICATION_JSON.value)
            self.logger.debug('[SERVICE] Service list created successfully')
        except Exception as e:
            self.logger.error(str(e))
            self.context.create_response(code=self.message_code.TASK_ERROR.value,
                                         message='Servis listesi oluşturulurken hata oluştu: ' + str(e),
                                         content_type=ContentType.APPLICATION_JSON.value)

    def get_service_status(self):
        (result_code, p_out, p_err) = self.execute("service --status-all")

        service_list = ServiceList()
        p_err = ' ' + p_err
        p_out = ' ' + p_out
        lines = p_out.split('\n')
        for line in lines:
            line_split = line.split(' ')
            if len(line_split) >= 5:
                proc = subprocess.Popen('chkconfig --list | grep 2:on | grep ' + line_split[len(line_split)-1], shell=True)
                auto = "INACTIVE"
                if proc.wait() == 0:
                    auto = "ACTIVE"
                if line_split[len(line_split)-4] == '+':
                    service_list.service_list.append(ServiceListItem(line_split[len(line_split)-1], "ACTIVE", auto))
                elif line_split[len(line_split)-4] == '-':
                    service_list.service_list.append(ServiceListItem(line_split[len(line_split)-1], "INACTIVE", auto))

        line_err = p_err.split(',')

        for line in line_err:
            line_split = line.split(' ')
            if len(line_split) >= 6:
                proc = subprocess.Popen('chkconfig --list | grep 2:on | grep ' + line_split[len(line_split)-1], shell=True)
                auto = "INACTIVE"
                if proc.wait() == 0:
                    auto = "ACTIVE"
                service_list.service_list.append(ServiceListItem(line_split[len(line_split)-1], "unknown", auto))

        result_service_list = json.dumps(service_list.__dict__, default=encode_service_object)
        self.logger.debug('[SERVICE]' + 'Service list: ' + str(result_service_list))
        return result_service_list


def handle_task(task, context):
    plugin = GetServices(task, context)
    plugin.handle_task()
