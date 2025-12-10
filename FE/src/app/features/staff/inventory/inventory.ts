import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import { 
  InventoryService, 
  InventoryItem, 
  InventoryResponse,
  InventorySearchRequest,
  StockUpdateRequest 
} from '../../../core/services/inventory.service';
import { InventoryCard } from '../../../shared/components/inventory-card/inventory-card';
import { StockUpdateModal } from '../../../shared/components/stock-update-modal/stock-update-modal';
import * as AOS from 'aos';

@Component({
  selector: 'app-inventory',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, InventoryCard, StockUpdateModal],
  templateUrl: './inventory.html',
  styleUrls: ['./inventory.scss']
})
export class Inventory implements OnInit {
  inventoryItems: InventoryItem[] = [];
  searchTerm: string = '';
  isLoading: boolean = true;

  // Pagination
  currentPage: number = 0;
  pageSize: number = 12;
  totalPages: number = 0;
  totalElements: number = 0;

  // View mode
  viewMode: 'grid' | 'list' = 'grid';

  // ✅ Stock Update Modal
  showStockModal: boolean = false;
  selectedItem: InventoryItem | null = null;

  Math = Math;

  constructor(
    private inventoryService: InventoryService,
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    AOS.init({
      duration: 500,
      easing: 'ease-in-out',
      once: true,
      mirror: false
    });
    this.loadInventory();
  }
  ngAfterViewInit() {
  AOS.refresh();
}

  loadInventory(page: number = 0): void {
    this.isLoading = true;
    this.currentPage = page;

    const request: InventorySearchRequest = {
      productName: this.searchTerm.trim(),
      page: page,
      size: this.pageSize,
      sortBy: 'lastUpdatedAt',
      sortDir: 'DESC'
    };

    this.inventoryService.getInventoryByBranch(request).subscribe({
      next: (response: InventoryResponse) => {
        this.inventoryItems = response.result.content;
        this.totalPages = response.result.totalPages;
        this.totalElements = response.result.totalElements;
        this.currentPage = response.result.number;
        this.isLoading = false;

        if (this.inventoryItems.length === 0) {
          this.toastr.info('Không tìm thấy sản phẩm nào', 'Thông báo');
        }
      },
      error: (error) => {
        console.error('❌ Error loading inventory:', error);
        this.isLoading = false;
        this.toastr.error('Không thể tải dữ liệu kho', 'Lỗi');
      }
    });
  }

  // ✅ Open Stock Update Modal
  onStockUpdate(item: InventoryItem): void {
    this.selectedItem = item;
    this.showStockModal = true;
  }

  // ✅ Close Modal
  onCloseModal(): void {
    this.showStockModal = false;
    this.selectedItem = null;
  }

  // ✅ Confirm Stock Update
  onConfirmStockUpdate(quantityChange: number): void {
    if (!this.selectedItem) return;

    const request: StockUpdateRequest = {
      productId: this.selectedItem.productId,
      quantityChange: quantityChange
    };

    this.inventoryService.updateStock(request).subscribe({
      next: (response) => {
        if (response.code === 0) {
          // Update item in list
          const index = this.inventoryItems.findIndex(
            item => item.productId === this.selectedItem!.productId
          );
          if (index !== -1) {
            this.inventoryItems[index] = response.result;
          }

          const action = quantityChange > 0 ? 'nhập' : 'xuất';
          this.toastr.success(
            `Đã ${action} ${Math.abs(quantityChange)} sản phẩm`,
            'Thành công'
          );
          this.onCloseModal();
        } else {
          this.toastr.error(response.message || 'Có lỗi xảy ra', 'Lỗi');
        }
      },
      error: (error) => {
        console.error('❌ Error updating stock:', error);
        this.toastr.error('Không thể cập nhật kho', 'Lỗi');
      }
    });
  }

  onSearch(): void {
    this.currentPage = 0;
    this.loadInventory(0);
  }

  clearSearch(): void {
    this.searchTerm = '';
    this.onSearch();
  }

  toggleViewMode(): void {
    this.viewMode = this.viewMode === 'grid' ? 'list' : 'grid';
  }

  onPageSizeChange(): void {
    this.currentPage = 0;
    this.loadInventory(0);
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.loadInventory(this.currentPage + 1);
      this.scrollToTop();
    }
  }

  previousPage(): void {
    if (this.currentPage > 0) {
      this.loadInventory(this.currentPage - 1);
      this.scrollToTop();
    }
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages) {
      this.loadInventory(page);
      this.scrollToTop();
    }
  }

  getPageNumbers(): number[] {
    const maxPagesToShow = 5;
    const pages: number[] = [];
    
    if (this.totalPages <= maxPagesToShow + 2) {
      for (let i = 1; i < this.totalPages - 1; i++) {
        pages.push(i);
      }
      return pages;
    }
    
    let startPage = Math.max(1, this.currentPage - 1);
    let endPage = Math.min(this.totalPages - 2, this.currentPage + 1);
    
    if (this.currentPage <= 2) {
      endPage = Math.min(maxPagesToShow - 1, this.totalPages - 2);
      startPage = 1;
    }
    
    if (this.currentPage >= this.totalPages - 3) {
      startPage = Math.max(1, this.totalPages - maxPagesToShow);
      endPage = this.totalPages - 2;
    }
    
    for (let i = startPage; i <= endPage; i++) {
      pages.push(i);
    }
    
    return pages;
  }

  get startIndex(): number {
    return this.currentPage * this.pageSize + 1;
  }

  get endIndex(): number {
    return Math.min((this.currentPage + 1) * this.pageSize, this.totalElements);
  }

  get availableCount(): number {
    return this.inventoryItems.filter(item => item.quantity >= 20).length;
  }

  get lowStockCount(): number {
    return this.inventoryItems.filter(item => item.quantity > 0 && item.quantity < 20).length;
  }

  get outOfStockCount(): number {
    return this.inventoryItems.filter(item => item.quantity === 0).length;
  }

  onViewDetail(item: InventoryItem): void {
    console.log('View detail:', item);
    this.toastr.info(`Xem chi tiết: ${item.productName}`, 'Thông tin');
  }

  onDelete(item: InventoryItem): void {
    if (confirm(`Bạn có chắc muốn xóa "${item.productName}" khỏi kho?`)) {
      console.log('Delete:', item);
      this.toastr.success(`Đã xóa: ${item.productName}`, 'Thành công');
    }
  }

  private scrollToTop(): void {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
}