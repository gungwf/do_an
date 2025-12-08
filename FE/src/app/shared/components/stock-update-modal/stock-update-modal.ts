import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { InventoryItem } from '../../../core/services/inventory.service';

@Component({
  selector: 'app-stock-update-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './stock-update-modal.html',
  styleUrl: './stock-update-modal.scss'
})
export class StockUpdateModal {
  @Input() item: InventoryItem | null = null;
  @Input() isVisible: boolean = false;
  @Output() close = new EventEmitter<void>();
  @Output() confirm = new EventEmitter<number>();

  quantityChange: number = 0;

  onClose(): void {
    this.quantityChange = 0;
    this.close.emit();
  }

  onConfirm(): void {
    if (this.quantityChange !== 0) {
      this.confirm.emit(this.quantityChange);
      this.quantityChange = 0;
    }
  }

  setQuickValue(value: number): void {
    this.quantityChange = value;
  }

  get newQuantity(): number {
    if (!this.item) return 0;
    return this.item.quantity + this.quantityChange;
  }

  get isValidQuantity(): boolean {
    return this.newQuantity >= 0;
  }

  get actionType(): string {
    if (this.quantityChange > 0) return 'Nhập kho';
    if (this.quantityChange < 0) return 'Xuất kho';
    return 'Cập nhật';
  }

  get actionClass(): string {
    if (this.quantityChange > 0) return 'text-success';
    if (this.quantityChange < 0) return 'text-danger';
    return 'text-muted';
  }
}