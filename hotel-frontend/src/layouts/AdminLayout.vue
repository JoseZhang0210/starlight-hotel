<template>
  <div class="admin-layout">
    <!-- =========================
         左側選單
         ========================= -->
    <aside class="sidebar">
      <div class="sidebar-logo">
        星澄飯店

        <span> 管理後台 </span>
      </div>

      <nav class="sidebar-nav">
        <!-- Dashboard -->
        <RouterLink to="/admin" class="sidebar-link">
          <LayoutDashboard :size="18" class="lucide-icon" />
          <span>Dashboard</span>
        </RouterLink>

        <!-- 商品管理 -->
        <RouterLink
          v-if="authStore.hasPermission('PRODUCT_MANAGE')"
          to="/admin/products"
          class="sidebar-link"
        >
          <ShoppingBag :size="18" class="lucide-icon" />
          <span>商品管理</span>
        </RouterLink>

        <!-- =========================
             餐廳管理群組
             ========================= -->
        <div
          v-if="authStore.hasPermission('RESTAURANT_MANAGE')"
          class="sidebar-group"
        >
          <button
            type="button"
            class="sidebar-group-title"
            @click="restaurantOpen = !restaurantOpen"
            :aria-expanded="restaurantOpen"
          >
            <span class="sidebar-title-with-icon">
              <Utensils :size="18" class="lucide-icon" />
              <span>餐廳管理</span>
            </span>

            <ChevronDown
              :size="16"
              class="sidebar-arrow"
              :class="{ 'is-open': restaurantOpen }"
            />
          </button>

          <div
            class="sidebar-submenu-wrapper"
            :class="{ 'is-open': restaurantOpen }"
          >
            <div class="sidebar-submenu-inner">
              <div class="sidebar-submenu">
                <RouterLink to="/admin/restaurants"> 餐廳資料管理 </RouterLink>
                <RouterLink to="/admin/restaurant-times">
                  餐廳時段管理
                </RouterLink>
                <RouterLink to="/admin/reservations"> 餐廳訂位管理 </RouterLink>
              </div>
            </div>
          </div>
        </div>

        <!-- ＝＝＝＝＝訂房管理＝＝＝＝＝ -->
        <div
          v-if="authStore.hasAnyPermission(['ROOM_MANAGE', 'BOOKING_MANAGE'])"
          class="sidebar-group"
        >
          <button
            type="button"
            class="sidebar-group-title"
            @click="roombookingOpen = !roombookingOpen"
            :aria-expanded="roombookingOpen"
          >
            <span class="sidebar-title-with-icon">
              <Bed :size="18" class="lucide-icon" />
              <span>訂房管理</span>
            </span>

            <ChevronDown
              :size="16"
              class="sidebar-arrow"
              :class="{ 'is-open': roombookingOpen }"
            />
          </button>

          <div
            class="sidebar-submenu-wrapper"
            :class="{ 'is-open': roombookingOpen }"
          >
            <div class="sidebar-submenu-inner">
              <div class="sidebar-submenu">
                <RouterLink
                  v-if="authStore.hasPermission('BOOKING_MANAGE')"
                  to="/admin/room-booking"
                  >訂房明細</RouterLink
                >
                <RouterLink
                  v-if="authStore.hasPermission('ROOM_MANAGE')"
                  to="/admin/room-status"
                  >房間管理</RouterLink
                >
                <RouterLink
                  v-if="authStore.hasPermission('ROOM_MANAGE')"
                  to="/admin/room-task"
                  >房務工單</RouterLink
                >
                <RouterLink
                  v-if="authStore.hasPermission('ROOM_MANAGE')"
                  to="/admin/room-types"
                  >房間類型</RouterLink
                >
                <RouterLink
                  v-if="authStore.hasPermission('ROOM_MANAGE')"
                  to="/admin/room-images"
                  >房型圖片</RouterLink
                >
                <RouterLink
                  v-if="authStore.hasPermission('BOOKING_MANAGE')"
                  to="/admin/booking-payments"
                  >付款紀錄</RouterLink
                >
                <RouterLink
                  v-if="
                    authStore.hasAnyPermission([
                      'ROOM_MANAGE',
                      'BOOKING_MANAGE',
                    ])
                  "
                  to="/admin/room-access-monitor"
                  >門禁卡片監控</RouterLink
                >
              </div>
            </div>
          </div>
        </div>
        <!-- ＝＝＝＝＝＝＝＝＝＝＝＝＝＝ -->

        <!-- 帳號管理群組 -->
        <div
          v-if="
            authStore.hasAnyPermission(['MEMBER_MANAGE', 'EMPLOYEE_MANAGE'])
          "
          class="sidebar-group"
        >
          <button
            type="button"
            class="sidebar-group-title"
            @click="accountOpen = !accountOpen"
            :aria-expanded="accountOpen"
          >
            <span class="sidebar-title-with-icon">
              <Users :size="18" class="lucide-icon" />
              <span>帳號管理</span>
            </span>

            <ChevronDown
              :size="16"
              class="sidebar-arrow"
              :class="{ 'is-open': accountOpen }"
            />
          </button>

          <div
            class="sidebar-submenu-wrapper"
            :class="{ 'is-open': accountOpen }"
          >
            <div class="sidebar-submenu-inner">
              <div class="sidebar-submenu">
                <RouterLink
                  v-if="authStore.hasPermission('MEMBER_MANAGE')"
                  to="/admin/members"
                  >會員管理</RouterLink
                >
                <RouterLink
                  v-if="authStore.hasPermission('EMPLOYEE_MANAGE')"
                  to="/admin/employees"
                  >員工管理</RouterLink
                >
              </div>
            </div>
          </div>
        </div>

        <!-- 訂單管理 -->
        <RouterLink
          v-if="authStore.hasPermission('ORDER_MANAGE')"
          to="/admin/orders"
          class="sidebar-link"
        >
          <Package :size="18" class="lucide-icon" />
          <span>訂單管理</span>
        </RouterLink>

        <!-- 優惠券管理 -->
        <RouterLink
          v-if="authStore.hasPermission('COUPON_MANAGE')"
          to="/admin/coupons"
          class="sidebar-link"
        >
          <Ticket :size="18" class="lucide-icon" />
          <span>優惠券管理</span>
        </RouterLink>

        <!-- 場地管理 -->
        <RouterLink
          v-if="authStore.hasPermission('VENUE_MANAGE')"
          to="/admin/venues"
          class="sidebar-link"
        >
          <Landmark :size="18" class="lucide-icon" />
          <span>場地管理</span>
        </RouterLink>

        <!-- 場地租借管理 -->
        <RouterLink
          v-if="authStore.hasPermission('VENUE_MANAGE')"
          to="/admin/rental"
          class="sidebar-link"
        >
          <CalendarCheck :size="18" class="lucide-icon" />
          <span>場地租借管理</span>
        </RouterLink>
      </nav>

      <!-- =========================
           Sidebar 底部
           ========================= -->
      <div class="sidebar-bottom">
        <RouterLink to="/" class="sidebar-back-link">
          <ArrowLeft :size="16" class="lucide-icon" />
          <span>回前台首頁</span>
        </RouterLink>
      </div>
    </aside>

    <!-- =========================
         右側內容
         ========================= -->
    <div class="admin-main">
      <!-- 後台 Header -->
      <header class="admin-header">
        <div>
          <h2>星澄飯店管理系統</h2>
        </div>

        <div class="admin-header-actions">
          <div class="admin-user">
            <span class="user-avatar-mini">
              <img
                v-if="authStore.avatarUrl && !hasAvatarError"
                :src="authStore.avatarUrl"
                alt="Avatar"
                class="avatar-mini-img"
                @error="hasAvatarError = true"
              />
              <span v-else>{{ userInitial }}</span>
            </span>
            <span class="user-greeting-text">{{ displayName }} 您好</span>
          </div>

          <RouterLink to="/" class="admin-home-button">
            <Home class="admin-home-icon" :size="18" aria-hidden="true" />
            <span>回首頁</span>
          </RouterLink>
        </div>
      </header>

      <!-- 子頁面 -->
      <main class="admin-content">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from "vue";
import { useRoute } from "vue-router";
import {
  LayoutDashboard,
  ShoppingBag,
  Utensils,
  Bed,
  Users,
  Package,
  Ticket,
  Landmark,
  CalendarCheck,
  ChevronDown,
  ArrowLeft,
  Home,
} from "@lucide/vue";
import { storeToRefs } from "pinia";
import { useAuthStore } from "@/stores/auth";

const route = useRoute();
const authStore = useAuthStore();
const { avatarUrl } = storeToRefs(authStore);
const hasAvatarError = ref(false);

watch(avatarUrl, () => {
  hasAvatarError.value = false;
});

const displayName = computed(() => {
  return authStore.name || "管理員";
});

const userInitial = computed(() => {
  if (authStore.name && authStore.name.trim().length > 0) {
    return authStore.name.trim().charAt(0);
  }
  return "管";
});

/*
 * 控制餐廳管理選單展開 / 收合
 *
 * true  = 預設展開
 * false = 預設收合
 */
const restaurantOpen = ref(true);
const roombookingOpen = ref(true);
const accountOpen = ref(true);
</script>

<style scoped>
.sidebar-link {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  box-sizing: border-box;
}

.sidebar-title-with-icon {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.sidebar-link .lucide-icon,
.sidebar-title-with-icon .lucide-icon,
.sidebar-back-link .lucide-icon {
  width: 20px;
  height: 20px;
  flex: 0 0 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.sidebar-arrow {
  width: 16px;
  height: 16px;
  flex: 0 0 16px;
  color: #aaa;
  transition: transform 0.25s ease;
  margin-left: auto;
}

.sidebar-arrow.is-open {
  transform: rotate(180deg);
}

/* =========================================
   子選單平滑滑動展開/收起動畫
   ========================================= */
.sidebar-submenu-wrapper {
  display: grid;
  grid-template-rows: 0fr;
  opacity: 0;
  visibility: hidden;
  transition:
    grid-template-rows 0.3s cubic-bezier(0.4, 0, 0.2, 1),
    opacity 0.25s ease,
    visibility 0.3s ease;
}

.sidebar-submenu-wrapper.is-open {
  grid-template-rows: 1fr;
  opacity: 1;
  visibility: visible;
}

.sidebar-submenu-inner {
  overflow: hidden;
}

.sidebar-back-link {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  box-sizing: border-box;
}

.lucide-icon {
  display: block;
  flex-shrink: 0;
}

.admin-header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  white-space: nowrap;
}

.admin-user {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: #fdfbf7;
  border: 1px solid #ebd9bf;
  padding: 6px 14px 6px 8px;
  border-radius: 24px;
  font-size: 14px;
  font-weight: 600;
  color: #4a3b2a;
}

.user-avatar-mini {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, #b58a46, #8f692f);
  color: #fff;
  font-size: 13px;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}

.avatar-mini-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.user-greeting-text {
  max-width: 180px;
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
}

.admin-home-button {
  min-height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 17px;
  box-sizing: border-box;
  border: 1px solid #9f763b;
  border-radius: 10px;
  background: linear-gradient(135deg, #a98043 0%, #87612e 100%);
  box-shadow: 0 4px 12px rgba(111, 83, 40, 0.2);
  color: #ffffff;
  font-size: 14px;
  font-weight: 700;
  line-height: 1;
  text-decoration: none;
  transition:
    background 0.2s ease,
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.admin-home-button:hover {
  border-color: #795421;
  background: linear-gradient(135deg, #b68d4d 0%, #76501f 100%);
  box-shadow: 0 6px 16px rgba(111, 83, 40, 0.3);
}

.admin-home-button:active {
  background: #76501f;
  box-shadow: 0 2px 7px rgba(111, 83, 40, 0.25);
}

.admin-home-button:focus-visible {
  outline: 3px solid rgba(181, 138, 70, 0.35);
  outline-offset: 3px;
}

.admin-home-icon {
  width: 18px;
  height: 18px;
  flex: 0 0 auto;
}

@media (max-width: 600px) {
  .admin-header-actions {
    gap: 8px;
  }

  .admin-home-button {
    min-height: 40px;
    padding: 8px 10px;
  }
}
</style>
