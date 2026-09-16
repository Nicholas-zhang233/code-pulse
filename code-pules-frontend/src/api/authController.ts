// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 此处后端没有提供注释 GET /auth/isLogin */
export async function isLogin(options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/auth/isLogin', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /auth/login */
export async function login(body: API.UserLoginRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseString>('/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /auth/logout */
export async function logout(options?: { [key: string]: any }) {
  return request<API.BaseResponseString>('/auth/logout', {
    method: 'POST',
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /auth/register */
export async function userRegister(
  body: API.UserRegisterRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseString>('/auth/register', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /auth/userInfo */
export async function getUserInfo(options?: { [key: string]: any }) {
  return request<API.BaseResponseLoginUserVO>('/auth/userInfo', {
    method: 'GET',
    ...(options || {}),
  })
}
