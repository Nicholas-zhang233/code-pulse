// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 此处后端没有提供注释 GET / */
export async function health(options?: { [key: string]: any }) {
  return request<string>('/', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 PUT / */
export async function health3(options?: { [key: string]: any }) {
  return request<string>('/', {
    method: 'PUT',
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST / */
export async function health2(options?: { [key: string]: any }) {
  return request<string>('/', {
    method: 'POST',
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 DELETE / */
export async function health5(options?: { [key: string]: any }) {
  return request<string>('/', {
    method: 'DELETE',
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 PATCH / */
export async function health4(options?: { [key: string]: any }) {
  return request<string>('/', {
    method: 'PATCH',
    ...(options || {}),
  })
}
