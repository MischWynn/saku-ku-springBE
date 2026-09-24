package com.binar.bc.saku_ku.util;

import java.time.LocalDate;
import java.time.Period;

// Umur minimal nasabah = umur minimal punya KTP. Dipakai CustomerAuthService (register & update
// profil) dan PengajuanService (syarat ngajuin pinjaman). Android punya aturan yang sama
// (util/Validators.kt) - kalau angkanya diubah, ubah di dua tempat.
public final class AgePolicy {

    public static final int MIN_AGE = 17;

    private AgePolicy() {
    }

    public static boolean isOldEnough(LocalDate tanggalLahir) {
        return tanggalLahir != null && Period.between(tanggalLahir, LocalDate.now()).getYears() >= MIN_AGE;
    }
}
