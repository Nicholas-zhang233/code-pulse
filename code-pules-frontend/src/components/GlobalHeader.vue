<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const logoVisible = ref(true)
const logoIndex = ref(0)

const logoUrls = ['/src/assets/logo.png', '/logo.png']
const logoUrl = computed(() => logoUrls[logoIndex.value])
const menuItems = [
  {
    key: '/',
    label: '首页',
    path: '/',
  },
  {
    key: '/about',
    label: '关于',
    path: '/about',
  },
]

const selectedKeys = computed(() => {
  const activeItem = menuItems.find((item) => item.path === route.path)
  return activeItem ? [activeItem.key] : []
})

const handleMenuClick = ({ key }: { key: string | number }) => {
  const targetItem = menuItems.find((item) => item.key === key)

  if (targetItem && targetItem.path !== route.path) {
    router.push(targetItem.path)
  }
}

const handleLogoError = () => {
  if (logoIndex.value < logoUrls.length - 1) {
    logoIndex.value += 1
    return
  }

  logoVisible.value = false
}
</script>

<template>
  <div class="global-header">
    <div class="global-header__brand">
      <img
        v-if="logoVisible"
        class="global-header__logo"
        :src="logoUrl"
        alt="Code Pulse logo"
        @error="handleLogoError"
      />
      <span class="global-header__title">Code Pulse</span>
    </div>

    <a-menu
      class="global-header__menu"
      mode="horizontal"
      :items="menuItems"
      :selected-keys="selectedKeys"
      @click="handleMenuClick"
    />

    <div class="global-header__actions">
      <a-button type="primary">登录</a-button>
    </div>
  </div>
</template>

<style scoped>
.global-header {
  display: flex;
  align-items: center;
  min-height: 64px;
  padding: 0 24px;
  gap: 24px;
}

.global-header__brand {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  min-width: 0;
  gap: 10px;
}

.global-header__logo {
  width: 32px;
  height: 32px;
  flex: 0 0 auto;
  object-fit: contain;
}

.global-header__title {
  color: #172033;
  font-size: 18px;
  font-weight: 700;
  line-height: 1;
  white-space: nowrap;
}

.global-header__menu {
  flex: 1 1 auto;
  min-width: 0;
  border-bottom: 0;
  overflow-x: auto;
  overflow-y: hidden;
}

.global-header__menu :deep(.ant-menu-overflow) {
  flex-wrap: nowrap;
}

.global-header__menu :deep(.ant-menu-item) {
  white-space: nowrap;
}

.global-header__actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
}

@media (max-width: 768px) {
  .global-header {
    padding: 0 12px;
    gap: 12px;
  }

  .global-header__title {
    font-size: 16px;
  }
}
</style>
