import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class AdminDashboard implements OnInit {
  stats = {
    doctors: 520,
    nurses: 6969,
    patients: 7509,
    pharmacists: 2110,
  };

  income = {
    today: 305,
    week: 1005,
    month: 5505,
    year: 155615,
  };

  constructor() {}

  ngOnInit(): void {
    // Load data from API here
  }
}








