import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

interface AccessibilitySettings {
  cursorSize: number;
  cursorColor: string;
  zoom: number;
  highContrast: boolean;
  dyslexiaFont: boolean;
  reduceAnimations: boolean;
}

@Component({
  selector: 'app-accessibility-toolbar',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './accessibility-toolbar.component.html',
  styleUrls: ['./accessibility-toolbar.component.css']
})
export class AccessibilityToolbarComponent implements OnInit {
  isOpen = false;
  
  settings: AccessibilitySettings = {
    cursorSize: 16,
    cursorColor: '#000000',
    zoom: 100,
    highContrast: false,
    dyslexiaFont: false,
    reduceAnimations: false
  };

  cursorColors = [
    { name: 'Black', value: '#000000' },
    { name: 'Red', value: '#FF0000' },
    { name: 'Blue', value: '#0000FF' },
    { name: 'Green', value: '#00FF00' },
    { name: 'Yellow', value: '#FFFF00' }
  ];

  ngOnInit(): void {
    this.loadSettings();
    this.applyAllSettings();
  }

  toggleToolbar(): void {
    this.isOpen = !this.isOpen;
  }

  // ========== CURSOR ==========
  updateCursorSize(size: number): void {
    this.settings.cursorSize = size;
    this.applyCursor();
    this.saveSettings();
  }

  updateCursorColor(color: string): void {
    this.settings.cursorColor = color;
    this.applyCursor();
    this.saveSettings();
  }

  private applyCursor(): void {
    const style = document.getElementById('custom-cursor-style') || this.createStyleElement('custom-cursor-style');
    const size = this.settings.cursorSize;
    const color = this.settings.cursorColor;
    
    style.textContent = `
      * {
        cursor: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 24 24"><path fill="${encodeURIComponent(color)}" d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>') ${size/2} ${size/2}, auto !important;
      }
      button, a, input, select, textarea {
        cursor: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 24 24"><path fill="${encodeURIComponent(color)}" d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>') ${size/2} ${size/2}, pointer !important;
      }
    `;
  }

  resetCursor(): void {
    this.settings.cursorSize = 16;
    this.settings.cursorColor = '#000000';
    const style = document.getElementById('custom-cursor-style');
    if (style) {
      style.textContent = '';
    }
    this.saveSettings();
  }

  // ========== ZOOM ==========
  updateZoom(zoom: number): void {
    this.settings.zoom = zoom;
    this.applyZoom();
    this.saveSettings();
  }

  private applyZoom(): void {
    document.body.style.zoom = `${this.settings.zoom}%`;
  }

  resetZoom(): void {
    this.settings.zoom = 100;
    this.applyZoom();
    this.saveSettings();
  }

  // ========== HIGH CONTRAST ==========
  toggleHighContrast(): void {
    this.settings.highContrast = !this.settings.highContrast;
    this.applyHighContrast();
    this.saveSettings();
  }

  private applyHighContrast(): void {
    if (this.settings.highContrast) {
      document.body.classList.add('high-contrast');
    } else {
      document.body.classList.remove('high-contrast');
    }
  }

  // ========== DYSLEXIA FONT ==========
  toggleDyslexiaFont(): void {
    this.settings.dyslexiaFont = !this.settings.dyslexiaFont;
    this.applyDyslexiaFont();
    this.saveSettings();
  }

  private applyDyslexiaFont(): void {
    if (this.settings.dyslexiaFont) {
      document.body.classList.add('dyslexia-font');
    } else {
      document.body.classList.remove('dyslexia-font');
    }
  }

  // ========== REDUCE ANIMATIONS ==========
  toggleReduceAnimations(): void {
    this.settings.reduceAnimations = !this.settings.reduceAnimations;
    this.applyReduceAnimations();
    this.saveSettings();
  }

  private applyReduceAnimations(): void {
    if (this.settings.reduceAnimations) {
      document.body.classList.add('reduce-animations');
    } else {
      document.body.classList.remove('reduce-animations');
    }
  }

  // ========== RESET ALL ==========
  resetAll(): void {
    this.settings = {
      cursorSize: 16,
      cursorColor: '#000000',
      zoom: 100,
      highContrast: false,
      dyslexiaFont: false,
      reduceAnimations: false
    };
    this.applyAllSettings();
    this.saveSettings();
  }

  // ========== STORAGE ==========
  private saveSettings(): void {
    localStorage.setItem('accessibility-settings', JSON.stringify(this.settings));
  }

  private loadSettings(): void {
    const saved = localStorage.getItem('accessibility-settings');
    if (saved) {
      this.settings = JSON.parse(saved);
    }
  }

  private applyAllSettings(): void {
    this.applyCursor();
    this.applyZoom();
    this.applyHighContrast();
    this.applyDyslexiaFont();
    this.applyReduceAnimations();
  }

  private createStyleElement(id: string): HTMLStyleElement {
    const style = document.createElement('style');
    style.id = id;
    document.head.appendChild(style);
    return style;
  }
}
