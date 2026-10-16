/**
 * 代码生成类型枚举
 */
export enum CodeGenTypeEnum {
  HTML = 'html',
  MULTI_FILE = 'multi_file',
  VUE_PROJECT = 'vue_project',
}

/**
 * 代码生成类型配置
 */
export const CODE_GEN_TYPE_CONFIG = {
  [CodeGenTypeEnum.HTML]: {
    label: '原生 HTML 模式',
    value: CodeGenTypeEnum.HTML,
  },
  [CodeGenTypeEnum.MULTI_FILE]: {
    label: '原生多文件模式',
    value: CodeGenTypeEnum.MULTI_FILE,
  },
  [CodeGenTypeEnum.VUE_PROJECT]: {
    label: 'Vue 项目模式',
    value: CodeGenTypeEnum.VUE_PROJECT,
  },
} as const

/**
 * 代码生成类型选项（用于下拉选择）
 */
export const CODE_GEN_TYPE_OPTIONS = Object.values(CODE_GEN_TYPE_CONFIG).map((config) => ({
  label: config.label,
  value: config.value,
}))

/**
 * 格式化代码生成类型
 * @param type 代码生成类型
 * @returns 格式化后的类型描述
 */
export const formatCodeGenType = (type: string | undefined): string => {
  if (!type) return '未知类型'

  const config = CODE_GEN_TYPE_CONFIG[type as CodeGenTypeEnum]
  return config ? config.label : type
}

/**
 * 获取所有代码生成类型
 */
export const getAllCodeGenTypes = () => {
  return Object.values(CodeGenTypeEnum)
}

/**
 * 检查是否为有效的代码生成类型
 * @param type 待检查的类型
 */
export const isValidCodeGenType = (type: string): type is CodeGenTypeEnum => {
  return Object.values(CodeGenTypeEnum).includes(type as CodeGenTypeEnum)
}

/**
 * 应用生成模式枚举
 * - NORMAL：一般模式（默认），走传统流式生成链路
 * - EXPERT：专家模式，走 LangGraph4j 工作流
 */
export enum GenModeEnum {
  NORMAL = 'normal',
  EXPERT = 'expert',
}

/**
 * 生成模式配置
 */
export const GEN_MODE_CONFIG = {
  [GenModeEnum.NORMAL]: {
    label: '一般模式',
    value: GenModeEnum.NORMAL,
    description: '快速生成，单次 AI 调用直接产出代码',
  },
  [GenModeEnum.EXPERT]: {
    label: '专家模式',
    value: GenModeEnum.EXPERT,
    description: '多阶段生成：自动搜集素材、智能路由、代码质检与重试、自动构建，耗时更长',
  },
} as const

/**
 * 默认生成模式
 */
export const DEFAULT_GEN_MODE = GenModeEnum.NORMAL

/**
 * 生成模式选项（用于分段选择器 / 下拉选择）
 */
export const GEN_MODE_OPTIONS = Object.values(GEN_MODE_CONFIG).map((config) => ({
  label: config.label,
  value: config.value,
}))

/**
 * 格式化生成模式
 * @param mode 生成模式
 * @returns 格式化后的模式描述
 */
export const formatGenMode = (mode: string | undefined): string => {
  if (!mode) return GEN_MODE_CONFIG[DEFAULT_GEN_MODE].label

  const config = GEN_MODE_CONFIG[mode as GenModeEnum]
  return config ? config.label : mode
}

/**
 * 获取生成模式说明文案
 * @param mode 生成模式
 */
export const getGenModeDescription = (mode: string | undefined): string => {
  const config = GEN_MODE_CONFIG[(mode || DEFAULT_GEN_MODE) as GenModeEnum]
  return config ? config.description : ''
}

/**
 * 是否为专家模式
 * @param mode 生成模式
 */
export const isExpertGenMode = (mode: string | undefined): boolean => {
  return mode === GenModeEnum.EXPERT
}
