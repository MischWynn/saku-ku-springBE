import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [],
  templateUrl: './login.html',
  styleUrl: './login.css',
})


export class LoginComponent {

  email = '';
  password = '';

  showPassword = false;

  constructor(private router: Router) {}

  togglePassword() {
    this.showPassword = !this.showPassword;
  }

  login() {
    if (
      this.email === 'admin@sakuku.com' &&
      this.password === 'admin123'
    ) {
      this.router.navigate(['/dashboard']);
    } else {
      alert('Email atau password salah!');
    }
  }
}