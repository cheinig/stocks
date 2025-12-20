import { Injectable } from '@angular/core';
import Keycloak from 'keycloak-js';

@Injectable({
  providedIn: 'root'
})
export class KeycloakService {
  private keycloak: Keycloak;
  private initialized = false;

  constructor() {
    this.keycloak = new Keycloak({
      url: 'https://auth.heinig.it',
      realm: 'heinis',
      clientId: 'stocks'
    });
  }

  async init(): Promise<boolean> {
    if (this.initialized) {
      return true;
    }

    try {
      const authenticated = await this.keycloak.init({
        onLoad: 'login-required',
        checkLoginIframe: false,
        enableLogging: true
      });

      this.initialized = true;

      if (authenticated) {
        this.startTokenRefresh();
      }

      return authenticated;
    } catch (error) {
      console.error('Keycloak initialization failed', error);
      throw error;
    }
  }

  getToken(): string | undefined {
    return this.keycloak.token;
  }

  isLoggedIn(): boolean {
    return !!this.keycloak.authenticated;
  }

  login(): Promise<void> {
    return this.keycloak.login();
  }

  logout(): Promise<void> {
    return this.keycloak.logout();
  }

  getUsername(): string | undefined {
    return this.keycloak.tokenParsed?.['preferred_username'];
  }

  getUserRoles(): string[] {
    const realmAccess = this.keycloak.tokenParsed?.['realm_access'];
    return realmAccess?.['roles'] || [];
  }

  hasRole(role: string): boolean {
    return this.getUserRoles().includes(role);
  }

  private startTokenRefresh(): void {
    setInterval(() => {
      this.keycloak.updateToken(70).catch(() => {
        console.error('Failed to refresh token');
      });
    }, 60000);
  }
}
