'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { useParams, useRouter } from 'next/navigation';
import ProtectedRoute from '@/components/common/ProtectedRoute';
import { evolutionService, Certificate } from '@/services/evolution';
import { coursesService } from '@/services/courses';
import { Course } from '@/types';
import {
  ArrowLeft,
  Award,
  Download,
  CheckCircle,
  AlertTriangle,
  Loader2,
  QrCode,
  Calendar,
  ShieldCheck
} from 'lucide-react';

export default function CertificatePage() {
  const params = useParams();
  const courseId = params.courseId as string;

  return (
    <ProtectedRoute>
      <CertificateContent courseId={courseId} />
    </ProtectedRoute>
  );
}

function CertificateContent({ courseId }: { courseId: string }) {
  const router = useRouter();
  const [cert, setCert] = useState<Certificate | null>(null);
  const [course, setCourse] = useState<Course | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchCertificateData = async () => {
    try {
      setLoading(true);
      setError(null);
      
      const [certRes, courseRes] = await Promise.all([
        evolutionService.getCertificate(courseId),
        coursesService.getCourse(courseId)
      ]);

      if (certRes.success) {
        setCert(certRes.data);
      } else {
        setError(certRes.message || 'Progress evaluation failed.');
      }
      
      if (courseRes.success) {
        setCourse(courseRes.data);
      }

    } catch (err: any) {
      setError(err.response?.data?.message || 'Certificate not unlocked yet. Ensure you completed at least 80% of the lessons.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCertificateData();
  }, [courseId]);

  const handlePrint = () => {
    window.print();
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-950 text-gray-200">
        <div className="flex flex-col items-center space-y-4">
          <Loader2 className="h-10 w-10 text-violet-500 animate-spin" />
          <span className="text-sm text-gray-400 font-medium">Compiling certificate credentials...</span>
        </div>
      </div>
    );
  }

  if (error || !cert || !course) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-gray-950 px-4 text-center text-gray-200">
        <AlertTriangle className="h-12 w-12 text-amber-500 mb-4 animate-pulse" />
        <h3 className="text-xl font-bold mb-2">Certificate Locked</h3>
        <p className="text-gray-400 text-sm max-w-sm mb-6">{error || 'You must study and complete lessons first.'}</p>
        <Link href={`/courses/${courseId}`} className="px-5 py-2.5 rounded-xl bg-violet-600 hover:bg-violet-700 text-sm font-semibold transition">
          Resume Course Study
        </Link>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-950 text-gray-100 flex flex-col pb-20 print:bg-white print:text-gray-950 print:pb-0">
      {/* Header (Hidden in Print) */}
      <header className="glass-panel sticky top-0 z-50 px-6 py-4 flex items-center justify-between border-b border-gray-900 bg-gray-950/80 print:hidden">
        <div className="flex items-center space-x-4">
          <Link href={`/courses/${courseId}`} className="p-2 hover:bg-gray-900 rounded-lg text-gray-400 hover:text-white transition">
            <ArrowLeft className="h-5 w-5" />
          </Link>
          <div>
            <h1 className="text-sm font-bold text-gray-200">Course Certificate</h1>
            <p className="text-[10px] text-violet-400 font-semibold uppercase">{course.title}</p>
          </div>
        </div>
        <button
          onClick={handlePrint}
          className="flex items-center space-x-2 px-4 py-2 rounded-xl bg-violet-600 hover:bg-violet-700 text-xs font-bold transition shadow-lg shadow-violet-900/20"
        >
          <Download className="h-4 w-4" />
          <span>Download PDF / Print</span>
        </button>
      </header>

      {/* Main Panel */}
      <main className="flex-1 flex items-center justify-center p-6 md:p-12 print:p-0">
        {/* Certificate Frame */}
        <div className="relative max-w-4xl w-full bg-gray-900/30 border-8 border-double border-violet-500/20 rounded-3xl p-10 md:p-16 flex flex-col items-center justify-between text-center overflow-hidden shadow-2xl print:border-violet-500 print:bg-white print:shadow-none print:rounded-none">
          {/* Decorative Corner Borders */}
          <div className="absolute top-0 left-0 w-24 h-24 border-t-4 border-l-4 border-violet-500/40 rounded-tl-2xl print:hidden"></div>
          <div className="absolute top-0 right-0 w-24 h-24 border-t-4 border-r-4 border-violet-500/40 rounded-tr-2xl print:hidden"></div>
          <div className="absolute bottom-0 left-0 w-24 h-24 border-b-4 border-l-4 border-violet-500/40 rounded-bl-2xl print:hidden"></div>
          <div className="absolute bottom-0 right-0 w-24 h-24 border-b-4 border-r-4 border-violet-500/40 rounded-br-2xl print:hidden"></div>

          {/* Ribbon Icon */}
          <div className="mb-6 flex flex-col items-center">
            <div className="w-20 h-20 bg-violet-600/10 rounded-full flex items-center justify-center border border-violet-500/30 mb-2">
              <Award className="h-10 w-10 text-violet-400" />
            </div>
            <p className="text-[10px] text-violet-400 tracking-[0.3em] font-extrabold uppercase">Certificate of Completion</p>
          </div>

          {/* Statement */}
          <div className="space-y-6 my-4 flex-1">
            <p className="text-gray-400 italic text-sm print:text-gray-600">This is proudly presented to</p>
            <h2 className="text-3xl md:text-5xl font-serif font-extrabold text-transparent bg-clip-text bg-gradient-to-r from-violet-300 to-indigo-100 print:text-gray-900">
              {cert.studentName}
            </h2>
            <p className="text-gray-400 text-sm max-w-lg mx-auto leading-relaxed print:text-gray-600">
              for successfully studying and satisfying all requirements, completions, and quizzes for the online AI-compiled course
            </p>
            <h3 className="text-xl md:text-2xl font-bold text-violet-400 print:text-violet-600">
              {cert.courseTitle}
            </h3>
            <p className="text-xs text-gray-500 font-semibold print:text-gray-600">
              Satisfying completion rating of <span className="text-emerald-400 font-bold">{cert.completionPercentage}%</span> on all learning tasks.
            </p>
          </div>

          {/* Validation section */}
          <div className="w-full mt-10 grid grid-cols-1 md:grid-cols-3 gap-8 pt-8 border-t border-gray-900/60 items-center justify-between print:border-gray-200">
            {/* Left side details */}
            <div className="space-y-2 text-left md:text-center">
              <div className="flex items-center justify-start md:justify-center space-x-1.5 text-xs text-gray-400 print:text-gray-600">
                <Calendar className="h-3.5 w-3.5 text-violet-400" />
                <span>Issue Date</span>
              </div>
              <p className="text-sm font-bold text-gray-200 print:text-gray-900">{cert.dateFormatted}</p>
            </div>

            {/* QR Verification details */}
            <div className="flex flex-col items-center space-y-2">
              <div className="relative p-1.5 bg-white rounded-lg border border-gray-800">
                <img
                  src={cert.verificationQrUrl}
                  alt="Verification QR Code"
                  className="w-24 h-24"
                />
              </div>
              <div className="flex items-center space-x-1 text-[9px] text-gray-500">
                <ShieldCheck className="h-3 w-3 text-emerald-400" />
                <span>Verify authenticity via QR Code</span>
              </div>
            </div>

            {/* Certificate ID details */}
            <div className="space-y-2 text-right md:text-center">
              <div className="flex items-center justify-end md:justify-center space-x-1.5 text-xs text-gray-400 print:text-gray-600">
                <QrCode className="h-3.5 w-3.5 text-violet-400" />
                <span>Certificate ID</span>
              </div>
              <p className="text-xs font-mono font-bold text-violet-300 print:text-gray-900 truncate">
                {cert.certificateId}
              </p>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
