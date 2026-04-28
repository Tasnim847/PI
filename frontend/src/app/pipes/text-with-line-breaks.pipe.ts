import { Pipe, PipeTransform } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

@Pipe({ name: 'textWithLineBreaks', standalone: true })
export class TextWithLineBreaksPipe implements PipeTransform {
  constructor(private sanitizer: DomSanitizer) {}

  transform(text: string): SafeHtml {
    if (!text) return '';
    // Convertir **texte** en <strong>texte</strong>
    let formatted = text.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
    // Convertir les sauts de ligne en <br>
    formatted = formatted.replace(/\n/g, '<br>');
    return this.sanitizer.sanitize(1, formatted) || '';
  }
}