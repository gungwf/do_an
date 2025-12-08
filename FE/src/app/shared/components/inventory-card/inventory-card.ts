import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { InventoryItem } from '../../../core/services/inventory.service';

@Component({
  selector: 'app-inventory-card',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './inventory-card.html',
  styleUrl: './inventory-card.scss'
})
export class InventoryCard {
  @Input({ required: true }) item!: InventoryItem;
  @Output() stockUpdate = new EventEmitter<InventoryItem>(); // ✅ Changed from edit
  @Output() delete = new EventEmitter<InventoryItem>();
  @Output() viewDetail = new EventEmitter<InventoryItem>();

  onStockUpdate(): void {
    this.stockUpdate.emit(this.item);
  }

  onDelete(): void {
    this.delete.emit(this.item);
  }

  onViewDetail(): void {
    this.viewDetail.emit(this.item);
  }

  onImageError(event: any): void {
    event.target.src = 'assets/images/default-product.png';
  }

  get displayImage(): string {
    return this.item.imageUrl || 'assets/images/default-product.png';
  }

  get statusBadgeClass(): string {
    if (this.item.quantity === 0) return 'badge bg-danger';
    if (this.item.quantity < 10) return 'badge bg-warning';
    if (this.item.quantity < 20) return 'badge bg-info';
    return 'badge bg-success';
  }

  get statusText(): string {
    if (this.item.quantity === 0) return 'Hết hàng';
    if (this.item.quantity < 10) return 'Sắp hết';
    if (this.item.quantity < 20) return 'Cảnh báo';
    return 'Còn hàng';
  }
}