<template>
  <div class="room-access-monitor-page">
    <!-- ==================== 頁面頂部標題與連線狀態列 ==================== -->
    <header class="page-header">
      <div class="title-section">
        <div class="badge-tag">
          <Key :size="16" />
          <span>IoT 門禁物聯網系統</span>
        </div>
        <h1>智慧客房門禁與卡片即時監控看板</h1>
        <p class="subtitle">
          讀取 ESP32 RFID 讀卡機卡號，即時對應星澄飯店全館 1~120 間客房門禁權限
        </p>
      </div>

      <!-- 讀卡機連線控制區 -->
      <div class="connection-box">
        <div class="serial-status-indicator">
          <span class="status-dot" :class="{ connected: isSerialConnected, scanning: isLearning }"></span>
          <span class="status-label">
            讀卡機狀態：
            <strong>{{ serialStatusText }}</strong>
          </span>
        </div>

        <div class="connection-actions">
          <button
            v-if="!isSerialConnected"
            @click="connectSerial"
            :disabled="!isSerialSupported"
            class="btn btn-primary"
            :title="isSerialSupported ? '透過 USB 連線 ESP32' : '此瀏覽器不支援 Web Serial API'"
          >
            <Usb :size="18" />
            <span>連接 ESP32 讀卡機</span>
          </button>

          <button
            v-else
            @click="disconnectSerial"
            class="btn btn-outline-danger"
          >
            <WifiOff :size="18" />
            <span>中斷連線</span>
          </button>

          <button @click="loadRooms" class="btn btn-secondary" :disabled="loadingRooms">
            <RefreshCw :size="16" :class="{ spin: loadingRooms }" />
            <span>重新載入房間</span>
          </button>
        </div>
      </div>
    </header>

    <!-- 瀏覽器不支援 Web Serial 時的溫馨提示 -->
    <div v-if="!isSerialSupported" class="alert-box warning">
      <AlertTriangle :size="20" />
      <div>
        <strong>瀏覽器提醒：</strong>目前使用的瀏覽器不支援 Web Serial API（建議使用 Chrome、Edge 或 Opera）。您仍可使用下方的<strong>「手動模擬刷卡」</strong>功能進行完整體驗與測試！
      </div>
    </div>

    <!-- ==================== 模擬刷卡除錯工具列 ==================== -->
    <section class="simulator-toolbar">
      <div class="simulator-inner">
        <div class="sim-label">
          <Sliders :size="18" />
          <span>測試模擬器：</span>
        </div>
        <div class="sim-buttons">
          <button @click="simulateSwipe(cardA.cardId)" class="btn btn-sm btn-gold">
            ⚡ 模擬刷卡片 A ({{ cardA.cardId || '未設定' }})
          </button>
          <button @click="simulateSwipe(cardB.cardId)" class="btn btn-sm btn-cyan">
            ⚡ 模擬刷卡片 B ({{ cardB.cardId || '未設定' }})
          </button>
          <button @click="simulateSwipe('INVALID999')" class="btn btn-sm btn-gray">
            ⛔ 模擬刷無效卡 (未授權)
          </button>
        </div>

        <div class="sim-input-box">
          <input
            v-model="customInputCardId"
            placeholder="自訂卡號 UID..."
            @keyup.enter="simulateSwipe(customInputCardId)"
          />
          <button @click="simulateSwipe(customInputCardId)" class="btn btn-sm btn-primary">
            手動感應
          </button>
        </div>
      </div>
    </section>

    <!-- ==================== 2 張實體卡片配對卡片區 ==================== -->
    <section class="cards-deck-section">
      <h2 class="section-title">
        <CreditCard :size="20" />
        <span>已配對房卡配置（2 張實體卡）</span>
      </h2>

      <div class="cards-deck-grid">
        <!-- 卡片 A -->
        <div class="card-item card-a" :class="{ active: activeSwipedCard === cardA.cardId }">
          <div class="card-chip-header">
            <div class="card-badge gold">實體卡片 A</div>
            <div class="card-signal">
              <span class="live-tag" v-if="activeSwipedCard === cardA.cardId">🔓 門鎖解鎖中</span>
              <span class="idle-tag" v-else>待命</span>
            </div>
          </div>

          <div class="card-details">
            <div class="card-field">
              <label>RFID 卡號 / UID (Barcode)</label>
              <div class="field-with-action">
                <input
                  v-model="cardA.cardId"
                  placeholder="請輸入或感應卡號 (如 53A2F109)"
                  class="code-font"
                />
                <button
                  @click="startLearning('A')"
                  class="btn btn-xs"
                  :class="learningTarget === 'A' ? 'btn-warn' : 'btn-outline'"
                >
                  {{ learningTarget === 'A' ? '請刷卡感應中...' : '🎯 感應學習' }}
                </button>
              </div>
            </div>

            <div class="card-field">
              <label>綁定目標房號</label>
              <select v-model="cardA.boundRoomNumber">
                <option v-for="room in allRooms" :key="room.roomId" :value="room.roomNumber">
                  房號 {{ room.roomNumber }}（{{ room.floor }}F - {{ getRoomTypeName(room) }}）
                </option>
              </select>
            </div>

            <div class="card-footer-info">
              <span>上次刷卡時間：{{ cardA.lastSwipeTime || '尚未感應' }}</span>
              <button @click="simulateSwipe(cardA.cardId)" class="btn-link">測試感應</button>
            </div>
          </div>
        </div>

        <!-- 卡片 B -->
        <div class="card-item card-b" :class="{ active: activeSwipedCard === cardB.cardId }">
          <div class="card-chip-header">
            <div class="card-badge cyan">實體卡片 B</div>
            <div class="card-signal">
              <span class="live-tag" v-if="activeSwipedCard === cardB.cardId">🔓 門鎖解鎖中</span>
              <span class="idle-tag" v-else>待命</span>
            </div>
          </div>

          <div class="card-details">
            <div class="card-field">
              <label>RFID 卡號 / UID (Barcode)</label>
              <div class="field-with-action">
                <input
                  v-model="cardB.cardId"
                  placeholder="請輸入或感應卡號 (如 7B4C89D1)"
                  class="code-font"
                />
                <button
                  @click="startLearning('B')"
                  class="btn btn-xs"
                  :class="learningTarget === 'B' ? 'btn-warn' : 'btn-outline'"
                >
                  {{ learningTarget === 'B' ? '請刷卡感應中...' : '🎯 感應學習' }}
                </button>
              </div>
            </div>

            <div class="card-field">
              <label>綁定目標房號</label>
              <select v-model="cardB.boundRoomNumber">
                <option v-for="room in allRooms" :key="room.roomId" :value="room.roomNumber">
                  房號 {{ room.roomNumber }}（{{ room.floor }}F - {{ getRoomTypeName(room) }}）
                </option>
              </select>
            </div>

            <div class="card-footer-info">
              <span>上次刷卡時間：{{ cardB.lastSwipeTime || '尚未感應' }}</span>
              <button @click="simulateSwipe(cardB.cardId)" class="btn-link">測試感應</button>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ==================== 1~120 間客房矩陣視覺看板 ==================== -->
    <section class="rooms-matrix-section">
      <div class="matrix-header">
        <div class="matrix-title-group">
          <h2>
            <Layers :size="20" />
            <span>全館客房門禁矩陣看板 (共 {{ allRooms.length }} 間房)</span>
          </h2>
          <span class="matrix-hint">即時同步房間狀態與卡片對應・刷卡開門即刻連動</span>
        </div>

        <!-- 樓層過濾與搜尋 -->
        <div class="filter-controls">
          <div class="search-input-box">
            <Search :size="16" />
            <input v-model="searchQuery" placeholder="搜尋房號 (例如 10501)..." />
          </div>

          <div class="floor-tabs">
            <button
              :class="{ active: selectedFloor === 'ALL' }"
              @click="selectedFloor = 'ALL'"
            >
              全部 ({{ allRooms.length }})
            </button>
            <button
              v-for="floor in availableFloors"
              :key="floor"
              :class="{ active: selectedFloor === floor }"
              @click="selectedFloor = floor"
            >
              {{ floor }}F
            </button>
          </div>
        </div>
      </div>

      <!-- 載入中動畫 -->
      <div v-if="loadingRooms" class="loading-state">
        <div class="spinner"></div>
        <p>正在載入全館 120 間房間資料庫...</p>
      </div>

      <!-- 房間網格卡片 -->
      <div v-else class="room-grid">
        <div
          v-for="room in filteredRooms"
          :key="room.roomId"
          class="room-card"
          :class="{
            'is-bound-card-a': isBoundToCardA(room.roomNumber),
            'is-bound-card-b': isBoundToCardB(room.roomNumber),
            'is-unlocked': unlockedRoomNumber === room.roomNumber,
            'is-locked-room': isRoomLocked(room.roomNumber),
          }"
        >
          <!-- 門鎖狀態動畫遮罩 (開門成功) -->
          <div v-if="unlockedRoomNumber === room.roomNumber" class="unlock-overlay">
            <div class="unlock-wave"></div>
            <DoorOpen :size="32" class="unlock-icon" />
            <span class="unlock-text">已開門！</span>
          </div>

          <!-- 門鎖防禦鎖定遮罩 (5 分鐘鎖定) -->
          <div v-if="isRoomLocked(room.roomNumber)" class="lockout-overlay">
            <AlertTriangle :size="24" class="lockout-icon" />
            <span class="lockout-text">安全鎖定中</span>
            <span class="lockout-time">{{ getRoomLockRemaining(room.roomNumber) }}</span>
            <button @click.stop="handleResetLock(room.roomNumber)" class="btn-unlock-sm">
              解除
            </button>
          </div>

          <div class="room-card-top">
            <div class="room-number">{{ room.roomNumber }}</div>
            <span class="floor-badge">{{ room.floor }}F</span>
          </div>

          <div class="room-type-name">{{ getRoomTypeName(room) }}</div>

          <div class="room-status-badge" :class="getStatusClass(room.roomStatus)">
            {{ room.roomStatus || '可預訂' }}
          </div>

          <!-- 房卡配對標記 -->
          <div class="binding-tag" v-if="isBoundToCardA(room.roomNumber)">
            <Key :size="12" />
            <span>卡片 A ({{ cardA.cardId || '未設定' }})</span>
          </div>
          <div class="binding-tag b" v-else-if="isBoundToCardB(room.roomNumber)">
            <Key :size="12" />
            <span>卡片 B ({{ cardB.cardId || '未設定' }})</span>
          </div>
          <div class="binding-tag empty" v-else>
            <span>無綁定實體卡</span>
          </div>

          <!-- 快速手動開門測試按鈕 -->
          <button
            class="door-action-btn"
            @click="testUnlockRoom(room.roomNumber)"
            title="手動模擬解鎖"
          >
            <DoorClosed :size="14" v-if="unlockedRoomNumber !== room.roomNumber" />
            <DoorOpen :size="14" v-else />
            <span>{{ unlockedRoomNumber === room.roomNumber ? '開門中' : '開門' }}</span>
          </button>
        </div>
      </div>
    </section>

    <!-- ==================== 即時刷卡歷史紀錄面板 ==================== -->
    <section class="swipe-logs-section">
      <div class="logs-header">
        <h3>即時感應門禁事件日誌 (最近 15 筆)</h3>
        <button @click="swipeLogs = []" class="btn btn-xs btn-outline">清除紀錄</button>
      </div>

      <div class="logs-list">
        <div v-if="swipeLogs.length === 0" class="no-logs">
          尚無刷卡事件，請靠近讀卡機刷卡或使用上方模擬器進行測試。
        </div>
        <div
          v-for="(log, idx) in swipeLogs"
          :key="idx"
          class="log-row"
          :class="log.success ? 'log-success' : 'log-failed'"
        >
          <span class="log-time">{{ log.time }}</span>
          <span class="log-badge">{{ log.success ? '驗證成功' : '驗證失敗' }}</span>
          <span class="log-card">卡號：<code>{{ log.cardId }}</code></span>
          <span class="log-detail">{{ log.message }}</span>
        </div>
      </div>
    </section>

    <!-- ==================== 浮動 Toast 通知 ==================== -->
    <transition name="toast">
      <div v-if="toastMessage" class="toast-notification" :class="toastType">
        <CheckCircle2 :size="20" v-if="toastType === 'success'" />
        <AlertTriangle :size="20" v-else />
        <span>{{ toastMessage }}</span>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from "vue";
import {
  Usb,
  Key,
  DoorOpen,
  DoorClosed,
  CheckCircle2,
  AlertTriangle,
  RefreshCw,
  Sliders,
  Search,
  WifiOff,
  CreditCard,
  Layers,
} from "@lucide/vue";
import { roomApi } from "@/api/roomApi";
import { roomTypeApi } from "@/api/roomTypeApi";

// ==========================================
// 1. 狀態管理與變數
// ==========================================
const isSerialSupported = ref(typeof navigator !== "undefined" && "serial" in navigator);
const isSerialConnected = ref(false);
const serialStatusText = ref("未連線 (可手動連線或使用模擬器)");
let serialPort = null;
let serialReader = null;
let serialWriter = null;

const allRooms = ref([]);
const roomTypes = ref([]);
const loadingRooms = ref(false);
const searchQuery = ref("");
const selectedFloor = ref("ALL");

// 2 張實體卡片綁定狀態
const cardA = reactive({
  name: "實體房卡 A",
  cardId: "53A2F109", // 預設測試 UID
  boundRoomNumber: "10501", // 預設對應第 1 間房
  lastSwipeTime: "",
});

const cardB = reactive({
  name: "實體房卡 B",
  cardId: "7B4C89D1", // 預設測試 UID
  boundRoomNumber: "10502", // 預設對應第 2 間房
  lastSwipeTime: "",
});

// 感應學習模式
const isLearning = ref(false);
const learningTarget = ref(null); // 'A' | 'B' | null

// 動態解鎖動畫狀態
const unlockedRoomNumber = ref(null);
const activeSwipedCard = ref(null);
const customInputCardId = ref("");
let unlockTimer = null;

// 日誌與 Toast
const swipeLogs = ref([]);
const toastMessage = ref("");
const toastType = ref("success");
let toastTimer = null;

// 門鎖防禦鎖定狀態 (3分鐘失敗超過5次，鎖定5分鐘)
const lockedRooms = reactive({}); // { "10501": { remainingSeconds: 300, timer: ... } }
const localFailureHistory = reactive({}); // { "10501": [timestamp1, timestamp2, ...] }

function isRoomLocked(roomNumber) {
  return !!(lockedRooms[roomNumber] && lockedRooms[roomNumber].remainingSeconds > 0);
}

function getRoomLockRemaining(roomNumber) {
  const secs = lockedRooms[roomNumber]?.remainingSeconds || 0;
  const m = Math.floor(secs / 60);
  const s = secs % 60;
  return `${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`;
}

function startLockoutCountdown(roomNumber, seconds) {
  if (lockedRooms[roomNumber]?.timer) {
    clearInterval(lockedRooms[roomNumber].timer);
  }
  lockedRooms[roomNumber] = {
    remainingSeconds: seconds,
    timer: setInterval(() => {
      if (lockedRooms[roomNumber] && lockedRooms[roomNumber].remainingSeconds > 1) {
        lockedRooms[roomNumber].remainingSeconds--;
      } else {
        if (lockedRooms[roomNumber]?.timer) {
          clearInterval(lockedRooms[roomNumber].timer);
        }
        delete lockedRooms[roomNumber];
        showToast(`房號 ${roomNumber} 安全鎖定已解除，恢復正常感應`, "info");
      }
    }, 1000),
  };
}

async function handleResetLock(roomNumber) {
  try {
    await roomApi.resetRoomLockout(roomNumber);
  } catch (e) {
    console.warn("後端重設失敗，本地解除：", e);
  }
  if (lockedRooms[roomNumber]?.timer) {
    clearInterval(lockedRooms[roomNumber].timer);
  }
  delete lockedRooms[roomNumber];
  localFailureHistory[roomNumber] = [];
  showToast(`房號 ${roomNumber} 門鎖已手動重設解除鎖定！`, "success");
}

// ==========================================
// 2. 樓層過濾與搜尋計算屬性
// ==========================================
const availableFloors = computed(() => {
  const floors = new Set(allRooms.value.map((r) => r.floor).filter(Boolean));
  return Array.from(floors).sort((a, b) => a - b);
});

const filteredRooms = computed(() => {
  return allRooms.value.filter((room) => {
    const matchFloor =
      selectedFloor.value === "ALL" || Number(room.floor) === Number(selectedFloor.value);
    const matchQuery =
      !searchQuery.value.trim() ||
      String(room.roomNumber).includes(searchQuery.value.trim());
    return matchFloor && matchQuery;
  });
});

function isBoundToCardA(roomNumber) {
  return String(cardA.boundRoomNumber) === String(roomNumber);
}

function isBoundToCardB(roomNumber) {
  return String(cardB.boundRoomNumber) === String(roomNumber);
}

function getRoomTypeName(room) {
  const typeId = room.roomTypeId ?? room.room_type_id;
  if (!typeId) return "標準客房";
  const found = roomTypes.value.find((rt) => Number(rt.roomTypeId) === Number(typeId));
  return found ? (found.typeName ?? found.type_name) : `房型 #${typeId}`;
}

function getStatusClass(status) {
  if (status === "可預訂") return "available";
  if (status === "已入住") return "occupied";
  if (status === "清潔中" || status === "退房待清潔") return "cleaning";
  if (status === "維修中") return "maintenance";
  return "disabled";
}

function showToast(msg, type = "success") {
  toastMessage.value = msg;
  toastType.value = type;
  if (toastTimer) clearTimeout(toastTimer);
  toastTimer = setTimeout(() => {
    toastMessage.value = "";
  }, 3500);
}

// ==========================================
// 3. Web Serial API 讀卡機串接
// ==========================================
async function connectSerial() {
  if (!isSerialSupported.value) {
    showToast("瀏覽器不支援 Web Serial API，請使用 Chrome / Edge", "error");
    return;
  }

  try {
    serialStatusText.value = "請在瀏覽器彈跳窗選擇 ESP32 序列埠...";
    serialPort = await navigator.serial.requestPort();
    await serialPort.open({ baudRate: 115200 });

    isSerialConnected.value = true;
    serialStatusText.value = "已連接 ESP32 (115200 Baud)";
    showToast("成功連接 ESP32 讀卡機！請靠近感應卡片", "success");

    readSerialLoop();
  } catch (err) {
    console.error("序列埠開啟失敗：", err);
    isSerialConnected.value = false;
    serialStatusText.value = "連線失敗或使用者取消";
    showToast("序列埠連線失敗：" + (err.message || err), "error");
  }
}

async function readSerialLoop() {
  const textDecoder = new TextDecoderStream();
  const readableStreamClosed = serialPort.readable.pipeTo(textDecoder.writable);
  serialReader = textDecoder.readable.getReader();

  let buffer = "";

  try {
    while (isSerialConnected.value) {
      const { value, done } = await serialReader.read();
      if (done) break;
      if (value) {
        buffer += value;
        const lines = buffer.split("\n");
        buffer = lines.pop(); // 保留尚未換行的最後一段

        for (const line of lines) {
          const cleanLine = line.trim();
          if (cleanLine) {
            handleSerialData(cleanLine);
          }
        }
      }
    }
  } catch (err) {
    console.error("序列埠讀取錯誤：", err);
  } finally {
    serialReader?.releaseLock();
  }
}

function handleSerialData(raw) {
  try {
    // 嘗試解析 JSON
    if (raw.startsWith("{") && raw.endsWith("}")) {
      const data = JSON.parse(raw);
      if (data.event === "CARD_SWIPE" && data.cardId) {
        handleCardSwipe(data.cardId);
      }
    } else if (raw.includes("卡片 UID")) {
      // 相容舊版印出格式
      const parts = raw.split(":");
      if (parts[1]) {
        const cleanUID = parts[1].replace(/\s+/g, "").toUpperCase();
        handleCardSwipe(cleanUID);
      }
    }
  } catch (err) {
    console.warn("無法解析 Serial 資料：", raw, err);
  }
}

async function sendToSerial(commandJson) {
  if (!serialPort || !serialPort.writable) return;
  try {
    const textEncoder = new TextEncoder();
    const writer = serialPort.writable.getWriter();
    await writer.write(textEncoder.encode(commandJson + "\n"));
    writer.releaseLock();
  } catch (err) {
    console.warn("回傳至 ESP32 失敗：", err);
  }
}

async function disconnectSerial() {
  try {
    if (serialReader) {
      await serialReader.cancel();
      serialReader = null;
    }
    if (serialPort) {
      await serialPort.close();
      serialPort = null;
    }
  } catch (err) {
    console.error("關閉序列埠錯誤：", err);
  } finally {
    isSerialConnected.value = false;
    serialStatusText.value = "已中斷連線";
    showToast("已中斷讀卡機連線", "info");
  }
}

// ==========================================
// 4. 卡片感應學習與驗證開門核心邏輯
// ==========================================
function startLearning(target) {
  if (learningTarget.value === target) {
    learningTarget.value = null;
    isLearning.value = false;
    return;
  }
  learningTarget.value = target;
  isLearning.value = true;
  showToast(`請在讀卡機感應要綁定給「卡片 ${target}」的實體卡...`, "info");
}

async function handleCardSwipe(scannedCardId) {
  const cardId = String(scannedCardId).trim().toUpperCase();
  const timeNow = new Date().toLocaleTimeString();

  // 若處於感應學習模式
  if (learningTarget.value) {
    if (learningTarget.value === "A") {
      cardA.cardId = cardId;
      cardA.lastSwipeTime = timeNow;
      showToast(`🎉 成功綁定實體卡片 A 卡號：${cardId}`, "success");
    } else if (learningTarget.value === "B") {
      cardB.cardId = cardId;
      cardB.lastSwipeTime = timeNow;
      showToast(`🎉 成功綁定實體卡片 B 卡號：${cardId}`, "success");
    }
    learningTarget.value = null;
    isLearning.value = false;
    return;
  }

  // 門禁比對邏輯
  let matchedRoom = null;
  let matchedCardLabel = null;

  if (cardA.cardId && cardId === cardA.cardId.toUpperCase()) {
    matchedRoom = cardA.boundRoomNumber;
    matchedCardLabel = "卡片 A";
    cardA.lastSwipeTime = timeNow;
    activeSwipedCard.value = cardA.cardId;
  } else if (cardB.cardId && cardId === cardB.cardId.toUpperCase()) {
    matchedRoom = cardB.boundRoomNumber;
    matchedCardLabel = "卡片 B";
    cardB.lastSwipeTime = timeNow;
    activeSwipedCard.value = cardB.cardId;
  }

  const targetRoom = matchedRoom || cardA.boundRoomNumber || "10501";
  const isValid = !!matchedRoom;

  // 檢查是否處於鎖定中
  if (isRoomLocked(targetRoom)) {
    sendToSerial(JSON.stringify({ command: "LOCKED", room: targetRoom }));
    showToast(`🚨 房號 ${targetRoom} 門鎖安全鎖定中，請等待冷卻結束！`, "error");
    swipeLogs.value.unshift({
      time: timeNow,
      cardId,
      success: false,
      message: `[安全鎖定拒絕] 房號 ${targetRoom} 鎖定中 (剩餘 ${getRoomLockRemaining(targetRoom)})`,
    });
    return;
  }

  try {
    const res = await roomApi.verifyAccess({
      roomNumber: targetRoom,
      cardId: cardId,
      isValidCard: isValid,
      deviceId: "ESP32_READER_01",
    });

    if (res.status === "LOCKED" || res.status === "LOCKOUT_TRIGGERED") {
      startLockoutCountdown(targetRoom, res.remainingLockSeconds || 300);
      sendToSerial(JSON.stringify({ command: "LOCKED", room: targetRoom }));
      showToast(res.message || "🚨 3分鐘內失敗超過5次，門鎖強制鎖定5分鐘！", "error");
      swipeLogs.value.unshift({
        time: timeNow,
        cardId,
        success: false,
        message: `[強制鎖定] 房號 ${targetRoom}：${res.message}`,
      });
      return;
    }

    if (res.status === "GRANTED") {
      triggerRoomUnlock(targetRoom, cardId, matchedCardLabel);
      sendToSerial(JSON.stringify({ command: "UNLOCK", room: targetRoom }));
      swipeLogs.value.unshift({
        time: timeNow,
        cardId,
        success: true,
        message: `驗證成功！開鎖 ${targetRoom} 號房 (${matchedCardLabel})`,
      });
      return;
    }

    // DENIED
    triggerAccessDenied(cardId, res.remainingAttempts);
    sendToSerial(JSON.stringify({ command: "DENIED" }));
    swipeLogs.value.unshift({
      time: timeNow,
      cardId,
      success: false,
      message: `未授權卡片（剩餘次數：${res.remainingAttempts ?? 0}）`,
    });
  } catch (err) {
    console.warn("後端 API 連線失敗，切換至前端本地防禦模式：", err);
    handleLocalFallbackSwipe(cardId, matchedRoom, matchedCardLabel, timeNow, targetRoom, isValid);
  }
}

function handleLocalFallbackSwipe(cardId, matchedRoom, matchedCardLabel, timeNow, targetRoom, isValid) {
  if (isValid) {
    localFailureHistory[targetRoom] = [];
    triggerRoomUnlock(targetRoom, cardId, matchedCardLabel);
    sendToSerial(JSON.stringify({ command: "UNLOCK", room: targetRoom }));
    swipeLogs.value.unshift({
      time: timeNow,
      cardId,
      success: true,
      message: `驗證成功！開鎖 ${targetRoom} 號房 (${matchedCardLabel})`,
    });
  } else {
    const nowTs = Date.now();
    if (!localFailureHistory[targetRoom]) localFailureHistory[targetRoom] = [];
    localFailureHistory[targetRoom] = localFailureHistory[targetRoom].filter((t) => nowTs - t <= 180000);
    localFailureHistory[targetRoom].push(nowTs);

    const failCount = localFailureHistory[targetRoom].length;
    if (failCount > 5) {
      startLockoutCountdown(targetRoom, 300);
      sendToSerial(JSON.stringify({ command: "LOCKED", room: targetRoom }));
      showToast(`🚨 3分鐘內失敗超過5次，房號 ${targetRoom} 強制鎖定5分鐘！`, "error");
      swipeLogs.value.unshift({
        time: timeNow,
        cardId,
        success: false,
        message: `[強制鎖定] 3分鐘內失敗超過5次，房號 ${targetRoom} 強制鎖定5分鐘！`,
      });
    } else {
      const remaining = 5 - failCount + 1;
      triggerAccessDenied(cardId, remaining);
      sendToSerial(JSON.stringify({ command: "DENIED" }));
      swipeLogs.value.unshift({
        time: timeNow,
        cardId,
        success: false,
        message: `未授權卡片，無法開門！（剩餘嘗試次數：${remaining}）`,
      });
    }
  }
}

function triggerRoomUnlock(roomNumber, cardId, cardLabel) {
  unlockedRoomNumber.value = roomNumber;
  showToast(`🎉 門禁開門成功！房號 [${roomNumber}] 門鎖已解鎖 (${cardLabel}：${cardId})`, "success");

  if (unlockTimer) clearTimeout(unlockTimer);
  unlockTimer = setTimeout(() => {
    unlockedRoomNumber.value = null;
    activeSwipedCard.value = null;
  }, 4000);
}

function triggerAccessDenied(cardId, remainingAttempts = null) {
  const attemptsMsg = remainingAttempts !== null ? ` (剩餘次數：${remainingAttempts})` : "";
  showToast(`⛔ 門禁驗證失敗：卡號 [${cardId}] 未註冊或未授權！${attemptsMsg}`, "error");
}

function simulateSwipe(cardId) {
  if (!cardId) {
    showToast("請先輸入卡號或在卡片設定卡號！", "error");
    return;
  }
  handleCardSwipe(cardId);
}

function testUnlockRoom(roomNumber) {
  triggerRoomUnlock(roomNumber, "MANUAL-TEST", "手動測試");
}

// ==========================================
// 5. 資料載入 (1~120 間房間與房型)
// ==========================================
async function loadRooms() {
  loadingRooms.value = true;
  try {
    const [roomsData, typesData] = await Promise.all([
      roomApi.getAllRooms().catch(() => null),
      roomTypeApi.getAllRoomTypes().catch(() => []),
    ]);

    roomTypes.value = Array.isArray(typesData) ? typesData : [];

    if (roomsData && Array.isArray(roomsData) && roomsData.length > 0) {
      allRooms.value = roomsData;
    } else {
      // 若後端尚未啟動或無資料，自動生成 120 間房間假資料以供無縫展示
      console.warn("未取得後端即時房間資料，載入全館 120 間預設種子資料...");
      allRooms.value = generateDefault120Rooms();
    }

    // 確保卡片 A 與 B 預設綁定有效房號
    if (allRooms.value.length > 0) {
      if (!cardA.boundRoomNumber) cardA.boundRoomNumber = allRooms.value[0].roomNumber;
      if (!cardB.boundRoomNumber) cardB.boundRoomNumber = allRooms.value[1]?.roomNumber || allRooms.value[0].roomNumber;
    }

    // 嘗試從後端讀取歷史刷卡日誌
    try {
      const logs = await roomApi.getRecentAccessLogs();
      if (Array.isArray(logs) && logs.length > 0) {
        swipeLogs.value = logs.map((l) => ({
          time: l.attemptTime ? new Date(l.attemptTime).toLocaleTimeString() : new Date().toLocaleTimeString(),
          cardId: l.cardId,
          success: l.accessResult === "SUCCESS",
          message:
            l.accessResult === "SUCCESS"
              ? `驗證成功！開鎖 ${l.roomNumber} 號房`
              : (l.failureReason || "驗證失敗"),
        }));
      }
    } catch (e) {
      // 忽略連線失敗
    }
  } catch (err) {
    console.error("載入房間資料失敗：", err);
    allRooms.value = generateDefault120Rooms();
  } finally {
    loadingRooms.value = false;
  }
}

// 產生標準 120 間房間種子（5F~10F，每層 20 間）
function generateDefault120Rooms() {
  const rooms = [];
  let id = 1;
  for (let floor = 5; floor <= 10; floor++) {
    for (let r = 1; r <= 20; r++) {
      const roomNum = `${floor}${r < 10 ? "0" + r : r}`;
      rooms.push({
        roomId: id++,
        roomNumber: roomNum,
        floor: floor,
        roomTypeId: ((r - 1) % 4) + 1,
        roomStatus: (id % 7 === 0) ? "已入住" : "可預訂",
      });
    }
  }
  return rooms;
}

onMounted(() => {
  loadRooms();
});

onBeforeUnmount(() => {
  disconnectSerial();
  if (unlockTimer) clearTimeout(unlockTimer);
  if (toastTimer) clearTimeout(toastTimer);
});
</script>

<style scoped>
.room-access-monitor-page {
  padding: 24px;
  max-width: 1560px;
  margin: 0 auto;
  color: #1e293b;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

/* ==================== 頁面頂部 ==================== */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 20px;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid #e2e8f0;
}

.badge-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: #f1f5f9;
  color: #475569;
  font-size: 13px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 999px;
  margin-bottom: 8px;
}

.title-section h1 {
  font-size: 26px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 6px 0;
}

.subtitle {
  color: #64748b;
  font-size: 14px;
  margin: 0;
}

/* 連線控制區 */
.connection-box {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
}

.serial-status-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #475569;
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background-color: #cbd5e1;
}

.status-dot.connected {
  background-color: #10b981;
  box-shadow: 0 0 8px rgba(16, 185, 129, 0.6);
}

.status-dot.scanning {
  background-color: #f59e0b;
  animation: pulse-dot 1s infinite;
}

@keyframes pulse-dot {
  0% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.4); opacity: 0.6; }
  100% { transform: scale(1); opacity: 1; }
}

.connection-actions {
  display: flex;
  gap: 10px;
}

/* 通用按鈕 */
.btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 16px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  transition: all 0.2s;
}

.btn-primary {
  background: #2563eb;
  color: white;
}
.btn-primary:hover {
  background: #1d4ed8;
}

.btn-secondary {
  background: #f1f5f9;
  color: #334155;
  border: 1px solid #cbd5e1;
}
.btn-secondary:hover {
  background: #e2e8f0;
}

.btn-outline-danger {
  background: #fff;
  color: #dc2626;
  border: 1px solid #fca5a5;
}
.btn-outline-danger:hover {
  background: #fee2e2;
}

.btn-sm {
  padding: 6px 12px;
  font-size: 13px;
}

.btn-xs {
  padding: 4px 8px;
  font-size: 12px;
  border-radius: 6px;
}

.spin {
  animation: spin 1s linear infinite;
}
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 提醒提示 */
.alert-box {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 20px;
  font-size: 14px;
}
.alert-box.warning {
  background-color: #fffbeb;
  border: 1px solid #fef3c7;
  color: #b45309;
}

/* ==================== 模擬器工具列 ==================== */
.simulator-toolbar {
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 10px;
  padding: 12px 18px;
  margin-bottom: 24px;
}

.simulator-inner {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 14px;
}

.sim-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  color: #475569;
  font-size: 14px;
}

.sim-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.btn-gold {
  background: #fef3c7;
  color: #92400e;
  border: 1px solid #fde68a;
}
.btn-gold:hover { background: #fde68a; }

.btn-cyan {
  background: #e0f2fe;
  color: #0369a1;
  border: 1px solid #bae6fd;
}
.btn-cyan:hover { background: #bae6fd; }

.btn-gray {
  background: #f1f5f9;
  color: #475569;
  border: 1px solid #cbd5e1;
}
.btn-gray:hover { background: #e2e8f0; }

.sim-input-box {
  display: flex;
  gap: 6px;
  margin-left: auto;
}
.sim-input-box input {
  padding: 6px 12px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  font-size: 13px;
  width: 170px;
}

/* ==================== 2 張卡片配對區 ==================== */
.cards-deck-section {
  margin-bottom: 30px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 14px;
}

.cards-deck-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(360px, 1fr));
  gap: 20px;
}

.card-item {
  background: #ffffff;
  border-radius: 14px;
  padding: 20px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
  transition: all 0.3s;
}

.card-item.card-a {
  border-top: 4px solid #f59e0b;
}

.card-item.card-b {
  border-top: 4px solid #0ea5e9;
}

.card-item.active {
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.4), 0 10px 15px -3px rgba(16, 185, 129, 0.2);
  border-color: #10b981;
}

.card-chip-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.card-badge {
  font-size: 13px;
  font-weight: 700;
  padding: 4px 12px;
  border-radius: 20px;
}
.card-badge.gold {
  background: #fef3c7;
  color: #92400e;
}
.card-badge.cyan {
  background: #e0f2fe;
  color: #0369a1;
}

.live-tag {
  background: #dcfce7;
  color: #15803d;
  font-size: 12px;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 6px;
  animation: pulse-dot 1s infinite;
}

.idle-tag {
  color: #94a3b8;
  font-size: 12px;
}

.card-field {
  margin-bottom: 14px;
}

.card-field label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  margin-bottom: 6px;
  text-transform: uppercase;
}

.card-field input,
.card-field select {
  width: 100%;
  padding: 9px 12px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  background: #f8fafc;
}

.card-field input:focus,
.card-field select:focus {
  border-color: #2563eb;
  background: #fff;
}

.field-with-action {
  display: flex;
  gap: 8px;
}

.code-font {
  font-family: "Courier New", Courier, monospace;
  font-weight: 700;
  letter-spacing: 1px;
}

.btn-warn {
  background: #f59e0b;
  color: white;
  border: none;
  animation: pulse-dot 1.2s infinite;
}

.card-footer-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #94a3b8;
  border-top: 1px solid #f1f5f9;
  padding-top: 10px;
  margin-top: 6px;
}

.btn-link {
  background: none;
  border: none;
  color: #2563eb;
  cursor: pointer;
  font-weight: 600;
  padding: 0;
}
.btn-link:hover { text-decoration: underline; }

/* ==================== 120 房間矩陣看板 ==================== */
.rooms-matrix-section {
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  padding: 24px;
  margin-bottom: 30px;
}

.matrix-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f1f5f9;
}

.matrix-title-group h2 {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 700;
  margin: 0 0 4px 0;
}

.matrix-hint {
  font-size: 13px;
  color: #64748b;
}

.filter-controls {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.search-input-box {
  display: flex;
  align-items: center;
  gap: 6px;
  border: 1px solid #cbd5e1;
  padding: 6px 12px;
  border-radius: 8px;
  background: #f8fafc;
}
.search-input-box input {
  border: none;
  outline: none;
  background: transparent;
  font-size: 13px;
  width: 150px;
}

.floor-tabs {
  display: flex;
  gap: 4px;
  background: #f1f5f9;
  padding: 4px;
  border-radius: 8px;
}

.floor-tabs button {
  padding: 5px 10px;
  border: none;
  background: transparent;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
  transition: all 0.2s;
}

.floor-tabs button.active {
  background: #ffffff;
  color: #0f172a;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

/* 網格排列 (120 間客房) */
.room-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(130px, 1fr));
  gap: 14px;
}

.room-card {
  position: relative;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  transition: all 0.25s ease;
  overflow: hidden;
}

.room-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 12px -2px rgba(0, 0, 0, 0.08);
}

/* 綁定卡片 A 房樣式 */
.room-card.is-bound-card-a {
  border: 2px solid #f59e0b;
  background: #fffbeb;
}

/* 綁定卡片 B 房樣式 */
.room-card.is-bound-card-b {
  border: 2px solid #0ea5e9;
  background: #f0f9ff;
}

/* 開門瞬間光暈與解鎖動畫 */
.room-card.is-unlocked {
  border-color: #10b981 !important;
  background-color: #ecfdf5 !important;
  animation: pulse-border 1s infinite alternate;
}

@keyframes pulse-border {
  0% { box-shadow: 0 0 0 2px #10b981; }
  100% { box-shadow: 0 0 14px 4px rgba(16, 185, 129, 0.7); }
}

.unlock-overlay {
  position: absolute;
  inset: 0;
  background: rgba(16, 185, 129, 0.95);
  color: white;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 10;
  animation: fadeIn 0.2s;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.unlock-text {
  font-size: 13px;
  font-weight: 700;
  margin-top: 4px;
}

/* 鎖定中房間樣式 */
.room-card.is-locked-room {
  border-color: #ef4444 !important;
  background-color: #fef2f2 !important;
  animation: pulse-locked 1s infinite alternate;
}

@keyframes pulse-locked {
  0% { box-shadow: 0 0 0 2px #ef4444; }
  100% { box-shadow: 0 0 14px 4px rgba(239, 68, 68, 0.7); }
}

.lockout-overlay {
  position: absolute;
  inset: 0;
  background: rgba(220, 38, 38, 0.95);
  color: white;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 10;
  padding: 6px;
  text-align: center;
  animation: fadeIn 0.2s;
}

.lockout-icon {
  margin-bottom: 2px;
}

.lockout-text {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.lockout-time {
  font-size: 13px;
  font-family: monospace;
  font-weight: 800;
  margin: 3px 0;
  background: rgba(0, 0, 0, 0.25);
  padding: 1px 6px;
  border-radius: 4px;
}

.btn-unlock-sm {
  background: #ffffff;
  color: #b91c1c;
  border: none;
  font-size: 11px;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
  margin-top: 2px;
}

.btn-unlock-sm:hover {
  background: #fee2e2;
}

.room-card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.room-number {
  font-size: 16px;
  font-weight: 800;
  color: #0f172a;
}

.floor-badge {
  font-size: 11px;
  background: #e2e8f0;
  color: #475569;
  padding: 2px 6px;
  border-radius: 4px;
}

.room-type-name {
  font-size: 11px;
  color: #64748b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 6px;
}

.room-status-badge {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 6px;
  border-radius: 4px;
  text-align: center;
  margin-bottom: 8px;
}

.available { background: #dcfce7; color: #166534; }
.occupied { background: #dbeafe; color: #1e40af; }
.cleaning { background: #fef3c7; color: #854d0e; }
.maintenance { background: #fee2e2; color: #991b1b; }
.disabled { background: #f1f5f9; color: #64748b; }

.binding-tag {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 10px;
  font-weight: 700;
  background: #fef3c7;
  color: #92400e;
  padding: 3px 6px;
  border-radius: 4px;
  margin-bottom: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.binding-tag.b {
  background: #e0f2fe;
  color: #0369a1;
}

.binding-tag.empty {
  background: transparent;
  color: #94a3b8;
  font-weight: normal;
}

.door-action-btn {
  margin-top: auto;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  padding: 4px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
  color: #334155;
  cursor: pointer;
  transition: all 0.2s;
}

.door-action-btn:hover {
  background: #f1f5f9;
  border-color: #94a3b8;
}

/* ==================== 刷卡日誌區 ==================== */
.swipe-logs-section {
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  padding: 20px;
}

.logs-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.logs-header h3 {
  font-size: 15px;
  font-weight: 700;
  margin: 0;
  color: #1e293b;
}

.logs-list {
  max-height: 220px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.no-logs {
  font-size: 13px;
  color: #94a3b8;
  text-align: center;
  padding: 20px 0;
}

.log-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 13px;
}

.log-row.log-success {
  background: #f0fdf4;
  border-left: 3px solid #16a34a;
}

.log-row.log-failed {
  background: #fef2f2;
  border-left: 3px solid #dc2626;
}

.log-time {
  color: #64748b;
  font-size: 12px;
}

.log-badge {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 4px;
}

.log-success .log-badge { background: #dcfce7; color: #15803d; }
.log-failed .log-badge { background: #fee2e2; color: #b91c1c; }

.log-card code {
  background: #e2e8f0;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: monospace;
}

.log-detail {
  font-weight: 600;
}

/* ==================== Toast 通知 ==================== */
.toast-notification {
  position: fixed;
  bottom: 24px;
  right: 24px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 20px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.15);
  z-index: 9999;
}

.toast-notification.success {
  background: #10b981;
  color: white;
}

.toast-notification.error {
  background: #ef4444;
  color: white;
}

.toast-notification.info {
  background: #0284c7;
  color: white;
}

.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(20px);
}
</style>
