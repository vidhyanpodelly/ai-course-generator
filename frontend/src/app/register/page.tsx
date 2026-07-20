'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { authService } from '@/services/auth';
import { GraduationCap, Mail, Lock, User, Loader2, AlertCircle, ShieldCheck, Eye, EyeOff } from 'lucide-react';

export default function RegisterPage() {
  const router = useRouter();
  
  // Step 1 states
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  
  // Step 2 states
  const [otpCode, setOtpCode] = useState('');
  const [step, setStep] = useState<1 | 2>(1);
  
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleInitiateSignUp = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (password.length < 6) {
      setError('Password must be at least 6 characters long.');
      return;
    }

    setLoading(true);

    try {
      // Dispatch Register OTP code via email
      await authService.initiateSignUp(email);
      setStep(2);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to dispatch verification code. Ensure your email is valid.');
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyOtpAndRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (otpCode.length < 4) {
      setError('OTP Code must be valid.');
      return;
    }

    setLoading(true);

    try {
      const response = await authService.registerWithOtp({
        email,
        otpCode,
        password,
        firstName,
        lastName
      });

      if (response.success && response.data) {
        localStorage.setItem('auth_token', response.data.token);
        localStorage.setItem('user_details', JSON.stringify(response.data));
        // Redirect to dashboard
        window.location.href = '/dashboard';
      } else {
        setError(response.message || 'OTP verification failed.');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'OTP Verification code is incorrect.');
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
            {step === 1 ? 'Create an account to begin learning' : 'Verify the code sent to your email'}
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

          {step === 1 ? (
            <form onSubmit={handleInitiateSignUp} className="space-y-5">
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <label className="text-xs font-semibold text-gray-300 tracking-wide uppercase">First Name</label>
                  <div className="relative">
                    <span className="absolute left-3.5 top-1/2 -translate-y-1/2 flex items-center text-gray-500 pointer-events-none">
                      <User className="h-4 w-4" />
                    </span>
                    <input
                      type="text"
                      required
                      placeholder="John"
                      value={firstName}
                      onChange={(e) => setFirstName(e.target.value)}
                      className="w-full pl-10 pr-3 py-2.5 rounded-xl border border-gray-800 bg-gray-900/30 text-gray-100 placeholder-gray-500 focus:outline-none focus:border-violet-500 focus:ring-2 focus:ring-violet-900/30 transition-all duration-300 text-sm"
                    />
                  </div>
                </div>

                <div className="space-y-2">
                  <label className="text-xs font-semibold text-gray-300 tracking-wide uppercase">Last Name</label>
                  <div className="relative">
                    <span className="absolute left-3.5 top-1/2 -translate-y-1/2 flex items-center text-gray-500 pointer-events-none">
                      <User className="h-4 w-4" />
                    </span>
                    <input
                      type="text"
                      required
                      placeholder="Doe"
                      value={lastName}
                      onChange={(e) => setLastName(e.target.value)}
                      className="w-full pl-10 pr-3 py-2.5 rounded-xl border border-gray-800 bg-gray-900/30 text-gray-100 placeholder-gray-500 focus:outline-none focus:border-violet-500 focus:ring-2 focus:ring-violet-900/30 transition-all duration-300 text-sm"
                    />
                  </div>
                </div>
              </div>

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
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className="w-full pl-10 pr-4 py-3 rounded-xl border border-gray-800 bg-gray-900/30 text-gray-100 placeholder-gray-500 focus:outline-none focus:border-violet-500 focus:ring-2 focus:ring-violet-900/30 transition-all duration-300"
                  />
                </div>
              </div>

              <div className="space-y-2">
                <label className="text-xs font-semibold text-gray-300 tracking-wide uppercase">Password</label>
                <div className="relative">
                  <span className="absolute left-3.5 top-1/2 -translate-y-1/2 flex items-center text-gray-500 pointer-events-none">
                    <Lock className="h-4 w-4" />
                  </span>
                  <input
                    type={showPassword ? "text" : "password"}
                    required
                    placeholder="Min. 6 characters"
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
                    <span>Sending code...</span>
                  </>
                ) : (
                  <span>Send OTP Verification Code</span>
                )}
              </button>
            </form>
          ) : (
            <form onSubmit={handleVerifyOtpAndRegister} className="space-y-5">
              <div className="space-y-2">
                <label className="text-xs font-semibold text-gray-300 tracking-wide uppercase">OTP Verification Code</label>
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
                <p className="text-[10px] text-gray-500 leading-normal">
                  Check your inbox for a 6-digit confirmation code. If not found, check spam folder.
                </p>
              </div>

              <button
                type="submit"
                disabled={loading}
                className="glow-button w-full py-3.5 px-4 rounded-xl bg-gradient-to-r from-violet-600 to-indigo-600 hover:from-violet-700 hover:to-indigo-700 font-semibold shadow-lg shadow-violet-900/20 text-white transition-all duration-300 flex items-center justify-center space-x-2 disabled:opacity-50"
              >
                {loading ? (
                  <>
                    <Loader2 className="h-5 w-5 animate-spin" />
                    <span>Registering...</span>
                  </>
                ) : (
                  <span>Verify Code & Sign Up</span>
                )}
              </button>

              <button
                type="button"
                onClick={() => setStep(1)}
                className="w-full text-center text-xs text-gray-400 hover:text-white transition"
              >
                Back to registration details
              </button>
            </form>
          )}

          <div className="mt-8 pt-6 border-t border-gray-900 text-center text-sm">
            <span className="text-gray-400">Already have an account? </span>
            <Link href="/login" className="text-violet-400 hover:text-violet-300 font-semibold transition-colors">
              Sign In
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
