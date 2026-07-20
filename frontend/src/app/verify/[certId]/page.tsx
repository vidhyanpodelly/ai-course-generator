'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import apiClient from '@/lib/api-client';
import { ApiResponse } from '@/types';
import { Certificate } from '@/services/evolution';
import {
  ShieldCheck,
  ShieldAlert,
  Loader2,
  Award,
  Calendar,
  User,
  GraduationCap
} from 'lucide-react';

export default function PublicVerificationPage() {
  const params = useParams();
  const certId = params.certId as string;

  const [cert, setCert] = useState<Certificate | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const verifyCertificate = async () => {
    try {
      setLoading(true);
      setError(null);
      
      const res = await apiClient.get<ApiResponse<Certificate>>(`/api/auth/verify/${certId}`);
      if (res.data.success) {
        setCert(res.data.data);
      } else {
        setError(res.data.message || 'Verification failed.');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Invalid or unregistered certificate ID.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (certId) {
      verifyCertificate();
    }
  }, [certId]);

  return (
    <div className="min-h-screen bg-gray-950 text-gray-100 flex flex-col items-center justify-center p-6 relative overflow-hidden">
      {/* Background radial glow */}
      <div className="absolute top-[45%] left-[50%] -translate-x-1/2 -translate-y-1/2 w-[550px] h-[550px] bg-violet-900/10 rounded-full blur-[110px]"></div>

      <div className="w-full max-w-xl relative z-10 space-y-8">
        {/* Brand header */}
        <div className="flex flex-col items-center">
          <Link href="/" className="flex items-center space-x-2 text-2xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-violet-400 to-indigo-200">
            <GraduationCap className="h-8 w-8 text-violet-500" />
            <span>Curricula.AI</span>
          </Link>
          <p className="text-gray-400 text-xs mt-2 uppercase tracking-[0.2em] font-extrabold">Public Credentials Registry</p>
        </div>

        {/* Verification Card */}
        <div className="glass-panel p-8 md:p-10 rounded-3xl border border-gray-800/80 shadow-2xl space-y-6 text-center">
          {loading ? (
            <div className="py-12 flex flex-col items-center space-y-4">
              <Loader2 className="h-10 w-10 text-violet-500 animate-spin" />
              <span className="text-xs text-gray-400 font-semibold">Validating certificate signature...</span>
            </div>
          ) : error ? (
            <div className="py-6 space-y-6">
              <div className="w-16 h-16 bg-red-650/10 rounded-full flex items-center justify-center border border-red-500/20 mx-auto">
                <ShieldAlert className="h-8 w-8 text-red-500 animate-bounce" />
              </div>
              <div>
                <h3 className="text-lg font-bold text-gray-200">Verification Failed</h3>
                <p className="text-xs text-gray-500 max-w-sm mx-auto mt-2 leading-relaxed">
                  The certificate reference ID <span className="font-mono text-red-400">{certId}</span> could not be authenticated in the official registry database.
                </p>
              </div>
              <div className="pt-4">
                <Link
                  href="/login"
                  className="px-5 py-2.5 bg-violet-600 hover:bg-violet-700 text-xs font-bold rounded-xl text-white transition"
                >
                  Return to login page
                </Link>
              </div>
            </div>
          ) : cert ? (
            <div className="space-y-6 text-center">
              <div className="w-16 h-16 bg-emerald-650/10 rounded-full flex items-center justify-center border border-emerald-500/20 mx-auto">
                <ShieldCheck className="h-8 w-8 text-emerald-400" />
              </div>
              
              <div>
                <span className="text-[10px] text-emerald-400 font-bold bg-emerald-500/5 px-2.5 py-1 rounded border border-emerald-500/10">
                  MATHEMATICALLY VERIFIED CERTIFICATE
                </span>
                <h3 className="text-xl font-extrabold text-gray-100 mt-4">Verified Graduate Credentials</h3>
              </div>

              <div className="border-t border-b border-gray-900 py-6 my-4 text-left space-y-4">
                <div className="flex justify-between items-center text-xs">
                  <span className="text-gray-500 font-semibold flex items-center space-x-1.5">
                    <User className="h-3.5 w-3.5 text-violet-400" />
                    <span>Graduate Name</span>
                  </span>
                  <span className="font-bold text-gray-200">{cert.studentName}</span>
                </div>
                
                <div className="flex justify-between items-center text-xs">
                  <span className="text-gray-500 font-semibold flex items-center space-x-1.5">
                    <Award className="h-3.5 w-3.5 text-violet-400" />
                    <span>Syllabus Title</span>
                  </span>
                  <span className="font-bold text-violet-400">{cert.courseTitle}</span>
                </div>

                <div className="flex justify-between items-center text-xs">
                  <span className="text-gray-500 font-semibold flex items-center space-x-1.5">
                    <Calendar className="h-3.5 w-3.5 text-violet-400" />
                    <span>Completion Date</span>
                  </span>
                  <span className="font-bold text-gray-200">{cert.dateFormatted}</span>
                </div>

                <div className="flex justify-between items-center text-xs">
                  <span className="text-gray-500 font-semibold flex items-center space-x-1.5">
                    <ShieldCheck className="h-3.5 w-3.5 text-violet-400" />
                    <span>Reference ID</span>
                  </span>
                  <span className="font-mono text-[10px] text-gray-400 truncate max-w-[200px]" title={cert.certificateId}>
                    {cert.certificateId}
                  </span>
                </div>
              </div>

              <p className="text-[10px] text-gray-500 leading-normal max-w-sm mx-auto">
                This verification certifies that the student satisfies all course modules and quizzes generated by Curricula.AI.
              </p>
            </div>
          ) : null}
        </div>
      </div>
    </div>
  );
}
