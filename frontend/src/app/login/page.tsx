'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/context/AuthContext';
import { authService } from '@/services/auth';
import { GraduationCap, Mail, Lock, Loader2, AlertCircle, ShieldCheck, ArrowLeft, Eye, EyeOff } from 'lucide-react';

export default function LoginPage() {
  const { login } = useAuth();
  const router = useRouter();
  
  // Standard login states
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  
  // Forgot password flow states
  const [showForgotFlow, setShowForgotFlow] = useState(false);
  const [forgotEmail, setForgotEmail] = useState('');
  const [forgotStep, setForgotStep] = useState<1 | 2>(1);
  const [otpCode, setOtpCode] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      await login({ email, password });
      router.push('/dashboard');
    } catch (err: any) {
      if (err.response && err.response.data && err.response.data.message) {
        setError(err.response.data.message);
      } else {
        setError('Login failed. Please verify your credentials.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleInitiateForgot = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccessMessage(null);
    setLoading(true);

    try {
      await authService.forgotPassword(forgotEmail);
      setForgotStep(2);
      setSuccessMessage('Password reset verification code dispatched.');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Could not find account for that email.');
    } finally {
      setLoading(false);
    }
  };

  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccessMessage(null);

    if (newPassword.length < 6) {
      setError('Password must be at least 6 characters long.');
      return;
    }

    setLoading(true);

    try {
      await authService.resetPassword({
        email: forgotEmail,
        otpCode,
        newPassword
      });
      setShowForgotFlow(false);
      setForgotStep(1);
      setSuccessMessage('Your password has been successfully reset. Sign in below.');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Verification failed. Code is incorrect or expired.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-950 px-4 relative overflow-hidden">
      {/* Background radial glow */}
      <div className="absolute top-[40%] left-[50%] -translate-x-1/2 -translate-y-1/2 w-[500px] h-[500px] bg-violet-900/10 rounded-full blur-[100px]"></div>

      <div className="w-full max-w-md relative z-10">
        {/* Brand Header */}
        <div className="flex flex-col items-center mb-8">
          <Link href="/" className="flex items-center space-x-2 text-2xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-violet-400 to-indigo-200">
            <GraduationCap className="h-8 w-8 text-violet-500" />
            <span>Curricula.AI</span>
          </Link>
          <p className="text-gray-400 text-sm mt-2">
            {showForgotFlow ? 'Recover your account password' : 'Sign in to resume your learning pathway'}
          </p>
        </div>

        {/* Card */}
        <div className="glass-card p-8 rounded-2xl border border-gray-800/80 shadow-2xl">
          {error && (
            <div className="p-4 mb-4 rounded-xl border border-red-500/20 bg-red-500/5 flex items-start space-x-3 text-red-400 text-sm">
              <AlertCircle className="h-5 w-5 shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          {successMessage && (
            <div className="p-4 mb-4 rounded-xl border border-emerald-500/20 bg-emerald-500/5 flex items-start space-x-3 text-emerald-400 text-sm">
              <span className="text-lg">✓</span>
              <span>{successMessage}</span>
            </div>
          )}

          {!showForgotFlow ? (
            <form onSubmit={handleSubmit} className="space-y-6">
              <div className="space-y-2">
                <label className="text-xs font-semibold text-gray-300 tracking-wide uppercase">Email Address</label>
                <div className="relative">
                  <span className="absolute left-3.5 top-1/2 -translate-y-1/2 flex items-center text-gray-500 pointer-events-none">
                    <Mail className="h-4 w-4" />
                  </span>
                  <input
                    type="email"
                    required
                    placeholder="name@example.com"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className="w-full pl-10 pr-4 py-3 rounded-xl border border-gray-800 bg-gray-900/30 text-gray-100 placeholder-gray-500 focus:outline-none focus:border-violet-500 focus:ring-2 focus:ring-violet-900/30 transition-all duration-300"
                  />
                </div>
              </div>

              <div className="space-y-2">
                <div className="flex justify-between items-center">
                  <label className="text-xs font-semibold text-gray-300 tracking-wide uppercase">Password</label>
                  <button
                    type="button"
                    onClick={() => { setShowForgotFlow(true); setError(null); setSuccessMessage(null); }}
                    className="text-xs text-violet-400 hover:text-violet-300 transition-colors"
                  >
                    Forgot Password?
                  </button>
                </div>
                <div className="relative">
                  <span className="absolute left-3.5 top-1/2 -translate-y-1/2 flex items-center text-gray-500 pointer-events-none">
                    <Lock className="h-4 w-4" />
                  </span>
                  <input
                    type={showPassword ? "text" : "password"}
                    required
                    placeholder="••••••••"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="w-full pl-10 pr-10 py-3 rounded-xl border border-gray-800 bg-gray-900/30 text-gray-100 placeholder-gray-500 focus:outline-none focus:border-violet-500 focus:ring-2 focus:ring-violet-900/30 transition-all duration-300"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3.5 top-1/2 -translate-y-1/2 flex items-center text-gray-500 hover:text-gray-300 transition-colors"
                  >
                    {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                </div>
              </div>

              <button
                type="submit"
                disabled={loading}
                className="glow-button w-full py-3.5 px-4 rounded-xl bg-gradient-to-r from-violet-600 to-indigo-600 hover:from-violet-700 hover:to-indigo-700 font-semibold shadow-lg shadow-violet-900/20 text-white transition-all duration-300 flex items-center justify-center space-x-2 disabled:opacity-50"
              >
                {loading ? (
                  <>
                    <Loader2 className="h-5 w-5 animate-spin" />
                    <span>Logging in...</span>
                  </>
                ) : (
                  <span>Sign In</span>
                )}
              </button>
            </form>
          ) : (
            <div>
              {forgotStep === 1 ? (
                <form onSubmit={handleInitiateForgot} className="space-y-6">
                  <div className="space-y-2">
                    <label className="text-xs font-semibold text-gray-300 tracking-wide uppercase">Email Address</label>
                    <div className="relative">
                      <span className="absolute left-3.5 top-1/2 -translate-y-1/2 flex items-center text-gray-500 pointer-events-none">
                        <Mail className="h-4 w-4" />
                      </span>
                      <input
                        type="email"
                        required
                        placeholder="john.doe@example.com"
                        value={forgotEmail}
                        onChange={(e) => setForgotEmail(e.target.value)}
                        className="w-full pl-10 pr-4 py-3 rounded-xl border border-gray-800 bg-gray-900/30 text-gray-100 placeholder-gray-500 focus:outline-none focus:border-violet-500 focus:ring-2 focus:ring-violet-900/30 transition-all duration-300"
                      />
                    </div>
                  </div>

                  <button
                    type="submit"
                    disabled={loading}
                    className="glow-button w-full py-3.5 px-4 rounded-xl bg-gradient-to-r from-violet-600 to-indigo-600 hover:from-violet-700 hover:to-indigo-700 font-semibold shadow-lg shadow-violet-900/20 text-white transition-all duration-300 flex items-center justify-center space-x-2 disabled:opacity-50"
                  >
                    {loading ? (
                      <Loader2 className="h-5 w-5 animate-spin" />
                    ) : (
                      <span>Send Recovery OTP</span>
                    )}
                  </button>
                </form>
              ) : (
                <form onSubmit={handleResetPassword} className="space-y-6">
                  <div className="space-y-2">
                    <label className="text-xs font-semibold text-gray-300 tracking-wide uppercase">Recovery OTP Code</label>
                    <div className="relative">
                      <span className="absolute left-3.5 top-1/2 -translate-y-1/2 flex items-center text-gray-500 pointer-events-none">
                        <ShieldCheck className="h-4 w-4 text-violet-400" />
                      </span>
                      <input
                        type="text"
                        required
                        maxLength={6}
                        placeholder="123456"
                        value={otpCode}
                        onChange={(e) => setOtpCode(e.target.value)}
                        className="w-full pl-10 pr-4 py-3 rounded-xl border border-gray-800 bg-gray-900/30 text-gray-100 placeholder-gray-500 tracking-[0.2em] text-center font-mono font-bold focus:outline-none focus:border-violet-500 focus:ring-2 focus:ring-violet-900/30 transition-all duration-300"
                      />
                    </div>
                  </div>

                  <div className="space-y-2">
                    <label className="text-xs font-semibold text-gray-300 tracking-wide uppercase">New Password</label>
                    <div className="relative">
                      <span className="absolute left-3.5 top-1/2 -translate-y-1/2 flex items-center text-gray-500 pointer-events-none">
                        <Lock className="h-4 w-4" />
                      </span>
                      <input
                        type={showNewPassword ? "text" : "password"}
                        required
                        placeholder="Min. 6 characters"
                        value={newPassword}
                        onChange={(e) => setNewPassword(e.target.value)}
                        className="w-full pl-10 pr-10 py-3 rounded-xl border border-gray-800 bg-gray-900/30 text-gray-100 placeholder-gray-500 focus:outline-none focus:border-violet-500 focus:ring-2 focus:ring-violet-900/30 transition-all duration-300"
                      />
                      <button
                        type="button"
                        onClick={() => setShowNewPassword(!showNewPassword)}
                        className="absolute right-3.5 top-1/2 -translate-y-1/2 flex items-center text-gray-500 hover:text-gray-300 transition-colors"
                      >
                        {showNewPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                      </button>
                    </div>
                  </div>

                  <button
                    type="submit"
                    disabled={loading}
                    className="glow-button w-full py-3.5 px-4 rounded-xl bg-gradient-to-r from-violet-600 to-indigo-600 hover:from-violet-700 hover:to-indigo-700 font-semibold shadow-lg shadow-violet-900/20 text-white transition-all duration-300 flex items-center justify-center space-x-2 disabled:opacity-50"
                  >
                    {loading ? (
                      <Loader2 className="h-5 w-5 animate-spin" />
                    ) : (
                      <span>Save and Update Password</span>
                    )}
                  </button>
                </form>
              )}

              <button
                type="button"
                onClick={() => { setShowForgotFlow(false); setForgotStep(1); setError(null); setSuccessMessage(null); }}
                className="w-full text-center text-xs text-gray-400 hover:text-white transition mt-6 flex items-center justify-center space-x-1"
              >
                <ArrowLeft className="h-3 w-3" />
                <span>Return to Login Inputs</span>
              </button>
            </div>
          )}

          {!showForgotFlow && (
            <div className="mt-8 pt-6 border-t border-gray-900 text-center text-sm">
              <span className="text-gray-400">Don't have an account? </span>
              <Link href="/register" className="text-violet-400 hover:text-violet-300 font-semibold transition-colors">
                Sign Up
              </Link>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
