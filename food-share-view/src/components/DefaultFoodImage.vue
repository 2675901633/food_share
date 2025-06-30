<template>
  <div class="default-food-image">
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" class="food-icon">
      <path d="M12,2A10,10 0 0,1 22,12A10,10 0 0,1 12,22A10,10 0 0,1 2,12A10,10 0 0,1 12,2M12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20A8,8 0 0,0 20,12A8,8 0 0,0 12,4M11,17H13V14H16V12H13V9H11V12H8V14H11V17Z" />
    </svg>
    <span>暂无图片</span>
  </div>
</template>

<script>
export default {
  name: 'DefaultFoodImage'
}
</script>

<style scoped>
.default-food-image {
  width: 100%;
  height: 100%;
  background-color: #f5f5f5;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #909399;
}

.food-icon {
  width: 40px;
  height: 40px;
  margin-bottom: 10px;
}
</style> 