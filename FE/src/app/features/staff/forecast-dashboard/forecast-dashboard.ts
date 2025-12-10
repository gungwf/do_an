import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import { 
  ForecastService, 
  BatchForecastResponse, 
  ForecastDataPoint 
} from '../../../core/services/forecastService';
import { 
  InventoryService, 
  InventoryItem 
} from '../../../core/services/inventory.service';
import { forkJoin } from 'rxjs';
import * as AOS from 'aos';

interface ProductForecastView {
  productId: string;
  productName: string;
  imageUrl?: string;
  currentStock: number;
  forecast7Days: number;
  avgDailyDemand: number;
  suggestedOrder: number;
  urgency: 'critical' | 'warning' | 'ok';
  trend: 'up' | 'down' | 'stable';
  forecastData?: ForecastDataPoint[];
  status: 'idle' | 'loading' | 'success' | 'error';
  errorMessage?: string;
  selected: boolean; // ✅ NEW: Track selection
}

@Component({
  selector: 'app-forecast-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './forecast-dashboard.html',
  styleUrl: './forecast-dashboard.scss'
})
export class ForecastDashboard implements OnInit {
  branchId: string = 'f7816c42-9d8c-4ac5-bd89-aea043049ff4';
  
  inventoryItems: InventoryItem[] = [];
  forecastResults: ProductForecastView[] = [];
  
  isLoadingInventory: boolean = false;
  isForecastingAll: boolean = false;
  
  // Settings
  historyDays: number = 365;
  safetyStockDays: number = 14;
  
  // Filters
  filterUrgency: 'all' | 'critical' | 'warning' | 'ok' = 'all';
  searchTerm: string = '';
  sortBy: 'name' | 'stock' | 'forecast' | 'urgency' = 'urgency';

  // Chart
  selectedProduct: ProductForecastView | null = null;
  showChartModal: boolean = false;

  constructor(
    private forecastService: ForecastService,
    private inventoryService: InventoryService,
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    AOS.init({
      duration: 400,
      easing: 'ease-in-out',
      once: true
    });
    this.loadInventory();
  }
  ngAfterViewInit() {
  AOS.refresh();
}
  loadInventory(): void {
    this.isLoadingInventory = true;
    
    this.inventoryService.getInventoryByBranch({
      page: 0,
      size: 100,
      sortBy: 'quantity',
      sortDir: 'ASC'
    }).subscribe({
      next: (response) => {
        this.inventoryItems = response.result.content;
        this.initializeForecastViews();
        this.isLoadingInventory = false;
      },
      error: (error) => {
        console.error('❌ Error loading inventory:', error);
        this.toastr.error('Không thể tải dữ liệu kho', 'Lỗi');
        this.isLoadingInventory = false;
      }
    });
  }

  initializeForecastViews(): void {
    this.forecastResults = this.inventoryItems.map(item => ({
      productId: item.productId,
      productName: item.productName,
      imageUrl: item.imageUrl,
      currentStock: item.quantity,
      forecast7Days: 0,
      avgDailyDemand: 0,
      suggestedOrder: 0,
      urgency: 'ok',
      trend: 'stable',
      status: 'idle',
      selected: false // ✅ Default not selected
    }));
  }

  // ✅ NEW: Toggle single product selection
  toggleProductSelection(product: ProductForecastView): void {
    product.selected = !product.selected;
  }

  // ✅ NEW: Select all visible products
  selectAllVisible(): void {
    const allSelected = this.filteredResults.every(p => p.selected);
    this.filteredResults.forEach(product => {
      product.selected = !allSelected;
    });
  }

  // ✅ NEW: Check if all visible are selected
  get allVisibleSelected(): boolean {
    return this.filteredResults.length > 0 && 
           this.filteredResults.every(p => p.selected);
  }

  // ✅ NEW: Get selected count
  get selectedCount(): number {
    return this.forecastResults.filter(p => p.selected).length;
  }

  // ✅ UPDATED: Forecast only selected products
  forecastSelectedProducts(): void {
    const selectedProducts = this.forecastResults.filter(p => p.selected);
    
    if (selectedProducts.length === 0) {
      this.toastr.warning('Vui lòng chọn ít nhất 1 sản phẩm', 'Cảnh báo');
      return;
    }

    this.isForecastingAll = true;
    
    // Set selected to loading, others to idle
    this.forecastResults = this.forecastResults.map(item => ({
      ...item,
      status: item.selected ? 'loading' : item.status
    }));

    const productIds = selectedProducts.map(item => item.productId);
    const batches = this.chunkArray(productIds, 50);
    
    console.log(`📊 Forecasting ${productIds.length} selected products in ${batches.length} batches`);
    
    const batchRequests = batches.map(batch =>
      this.forecastService.forecastMedicineBatch(this.branchId, batch, this.historyDays)
    );
    
    forkJoin(batchRequests).subscribe({
      next: (responses) => {
        const allResults: BatchForecastResponse = {};
        responses.forEach(response => Object.assign(allResults, response));
        
        this.processBatchResults(allResults);
        this.isForecastingAll = false;
        
        const successCount = selectedProducts.filter(p => {
          const result = this.forecastResults.find(r => r.productId === p.productId);
          return result?.status === 'success';
        }).length;
        
        this.toastr.success(
          `Đã dự báo thành công ${successCount}/${selectedProducts.length} sản phẩm`,
          'Hoàn thành'
        );
      },
      error: (error) => {
        console.error('❌ Batch forecast error:', error);
        this.toastr.error('Lỗi khi dự báo hàng loạt', 'Lỗi');
        this.isForecastingAll = false;
      }
    });
  }

  processBatchResults(results: BatchForecastResponse): void {
    this.forecastResults = this.forecastResults.map(item => {
      // ✅ Only update selected items
      if (!item.selected) {
        return item;
      }

      const result = results[item.productId];
      
      if (!result || !result.success) {
        return {
          ...item,
          status: 'error',
          errorMessage: result?.error || 'Không nhận được kết quả'
        };
      }
      
      const forecast7Days = result.forecast || [];
      const totalForecast = forecast7Days.reduce((sum, day) => sum + day.yhat, 0);
      const avgDaily = totalForecast / 7;
      
      const neededFor2Weeks = Math.ceil(avgDaily * this.safetyStockDays);
      const suggestedOrder = Math.max(0, neededFor2Weeks - item.currentStock);
      
      const urgency = this.calculateUrgency(item.currentStock, avgDaily);
      const trend = this.calculateTrend(forecast7Days);
      
      return {
        ...item,
        forecast7Days: Math.round(totalForecast),
        avgDailyDemand: Math.round(avgDaily * 10) / 10,
        suggestedOrder: suggestedOrder,
        urgency: urgency,
        trend: trend,
        forecastData: forecast7Days,
        status: 'success'
      };
    });
  }

  calculateUrgency(currentStock: number, avgDaily: number): 'critical' | 'warning' | 'ok' {
    const daysRemaining = currentStock / (avgDaily || 1);
    
    if (daysRemaining < 3) return 'critical';
    if (daysRemaining < 7) return 'warning';
    return 'ok';
  }

  calculateTrend(forecast: ForecastDataPoint[]): 'up' | 'down' | 'stable' {
    if (forecast.length < 4) return 'stable';
    
    const firstHalf = forecast.slice(0, 3).reduce((sum, day) => sum + day.yhat, 0) / 3;
    const secondHalf = forecast.slice(4).reduce((sum, day) => sum + day.yhat, 0) / 3;
    
    const diff = (secondHalf - firstHalf) / (firstHalf || 1);
    
    if (diff > 0.15) return 'up';
    if (diff < -0.15) return 'down';
    return 'stable';
  }

  chunkArray<T>(array: T[], size: number): T[][] {
    const chunks: T[][] = [];
    for (let i = 0; i < array.length; i += size) {
      chunks.push(array.slice(i, i + size));
    }
    return chunks;
  }

  viewForecastDetail(product: ProductForecastView): void {
    this.selectedProduct = product;
    this.showChartModal = true;
  }

  closeChartModal(): void {
    this.showChartModal = false;
    this.selectedProduct = null;
  }

  get filteredResults(): ProductForecastView[] {
    let results = [...this.forecastResults];
    
    if (this.filterUrgency !== 'all') {
      results = results.filter(item => item.urgency === this.filterUrgency);
    }
    
    if (this.searchTerm) {
      const search = this.searchTerm.toLowerCase();
      results = results.filter(item =>
        item.productName.toLowerCase().includes(search)
      );
    }
    
    results.sort((a, b) => {
      switch (this.sortBy) {
        case 'name':
          return a.productName.localeCompare(b.productName);
        case 'stock':
          return a.currentStock - b.currentStock;
        case 'forecast':
          return b.suggestedOrder - a.suggestedOrder;
        case 'urgency':
          const urgencyOrder = { 'critical': 0, 'warning': 1, 'ok': 2 };
          return urgencyOrder[a.urgency] - urgencyOrder[b.urgency];
        default:
          return 0;
      }
    });
    
    return results;
  }

  get totalProducts(): number {
    return this.forecastResults.filter(r => r.status === 'success').length;
  }

  get needRestock(): number {
    return this.forecastResults.filter(item => 
      item.status === 'success' && item.suggestedOrder > 0
    ).length;
  }

  get criticalProducts(): number {
    return this.forecastResults.filter(item => 
      item.status === 'success' && item.urgency === 'critical'
    ).length;
  }

  get warningProducts(): number {
    return this.forecastResults.filter(item => 
      item.status === 'success' && item.urgency === 'warning'
    ).length;
  }

  get totalSuggestedOrder(): number {
    return this.forecastResults
      .filter(r => r.status === 'success')
      .reduce((sum, item) => sum + item.suggestedOrder, 0);
  }
}