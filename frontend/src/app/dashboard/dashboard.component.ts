import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})

export class DashboardComponent {

  stats = [
    {
      title: 'Total Pengajuan',
      value: '1,284',
      icon: '📄',
      percentage: '+12%'
    },
    {
      title: 'Menunggu Review',
      value: '142',
      icon: '📋'
    },
    {
      title: 'Menunggu Approval',
      value: '56',
      icon: '✓'
    },
    {
      title: 'Siap Dicairkan',
      value: '29',
      icon: '💳'
    },
    {
      title: 'Total Dana Cair',
      value: 'Rp 1.4B',
      icon: '🏦'
    }
  ];

  activities = [
    {
      title: 'Approval Pencairan Dana',
      description: 'Pengajuan #PL-202401 telah disetujui untuk pencairan sebesar Rp15.000.000',
      time: 'Hari ini, 09:24 AM'
    },
    {
      title: 'Review Anggaran Baru',
      description: 'Sistem menerima 12 pengajuan baru yang memerlukan review',
      time: 'Hari ini, 08:15 AM'
    },
    {
      title: 'Approval Selesai',
      description: 'Pengajuan #PL-202398 berhasil disetujui',
      time: 'Kemarin, 16:42 PM'
    }
  ];

}