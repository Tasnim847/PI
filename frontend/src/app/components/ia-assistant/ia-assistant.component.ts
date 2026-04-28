import { Component, ElementRef, ViewChild, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IAAssistantService, ChatResponse } from '../../services/ia-assistant.service';
import { TextWithLineBreaksPipe } from '../../pipes/text-with-line-breaks.pipe';
interface Message {
  id: string;
  text: string;
  isUser: boolean;
  timestamp: string;
  suggestions?: string[];
  isTyping?: boolean;
}

@Component({
  selector: 'app-ia-assistant',
  standalone: true,
  imports: [CommonModule, FormsModule ,TextWithLineBreaksPipe],
  templateUrl: './ia-assistant.component.html',
  styleUrls: ['./ia-assistant.component.css']
})
export class IAAssistantComponent implements AfterViewChecked {
  @ViewChild('chatMessages') private chatMessagesContainer!: ElementRef;
  
  isOpen = false;
  messages: Message[] = [];
  newMessage = '';
  isLoading = false;

  constructor(private iaAssistantService: IAAssistantService) {
    this.initializeWelcomeMessage();
  }

  ngAfterViewChecked() {
    this.scrollToBottom();
  }

  private scrollToBottom(): void {
    if (this.chatMessagesContainer) {
      this.chatMessagesContainer.nativeElement.scrollTop = this.chatMessagesContainer.nativeElement.scrollHeight;
    }
  }

  initializeWelcomeMessage() {
    this.messages.push({
      id: this.generateId(),
      text: '👋 **Bonjour !**\n\nJe suis SurFin, votre assistant bancaire intelligent. Voici ce que je peux faire pour vous :\n\n💰 **Consulter votre solde**\n📊 **Voir vos transactions**\n💸 **Faire un virement**\n📝 **Créer une réclamation**\n\nComment puis-je vous aider aujourd\'hui ?',
      isUser: false,
      timestamp: new Date().toLocaleTimeString(),
      suggestions: ['💰 Solde', '📊 Transactions', '💸 Virement', '📝 Réclamation']
    });
  }

  toggleChat() {
    this.isOpen = !this.isOpen;
    if (this.isOpen) {
      setTimeout(() => this.scrollToBottom(), 100);
    }
  }

  sendMessage() {
    if (!this.newMessage.trim()) return;

    const userMessage: Message = {
      id: this.generateId(),
      text: this.newMessage,
      isUser: true,
      timestamp: new Date().toLocaleTimeString()
    };
    
    this.messages.push(userMessage);
    const messageToSend = this.newMessage;
    this.newMessage = '';
    this.isLoading = true;

    // Message de typing
    const typingMessage: Message = {
      id: this.generateId(),
      text: '',
      isUser: false,
      timestamp: new Date().toLocaleTimeString(),
      isTyping: true
    };
    this.messages.push(typingMessage);
    this.scrollToBottom();

    this.iaAssistantService.sendMessage(messageToSend).subscribe({
      next: (response: ChatResponse) => {
        this.messages = this.messages.filter(m => !m.isTyping);
        
        const botMessage: Message = {
          id: this.generateId(),
          text: response.response,
          isUser: false,
          timestamp: response.timestamp,
          suggestions: response.suggestions
        };
        this.messages.push(botMessage);
        this.isLoading = false;
        this.scrollToBottom();
      },
      error: (err) => {
        this.messages = this.messages.filter(m => !m.isTyping);
        this.messages.push({
          id: this.generateId(),
          text: '❌ Désolé, une erreur est survenue. Veuillez réessayer.',
          isUser: false,
          timestamp: new Date().toLocaleTimeString()
        });
        this.isLoading = false;
        this.scrollToBottom();
      }
    });
  }

  useSuggestion(suggestion: string) {
    this.newMessage = suggestion;
    this.sendMessage();
  }

  resetConversation() {
    this.iaAssistantService.resetConversation();
    this.messages = [];
    this.initializeWelcomeMessage();
  }

  private generateId(): string {
    return Date.now().toString() + '-' + Math.random().toString(36).substr(2, 9);
  }
}