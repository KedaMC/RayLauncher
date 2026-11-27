/*
 * Zalith Launcher 2
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.components.jre

/** A single downloadable runtime archive */
data class RuntimePackage(val sha1: String, val size: Long)

/**
 * @param version Written into the installed runtime, bump it to make launchers reinstall
 * @param packages Archives by file name
 */
data class RuntimeRelease(val version: String, val packages: Map<String, RuntimePackage>)

/**
 * Runtimes are not bundled, they are downloaded from the `runtimes` directory of the repository.
 * [BASE_URL] is pinned to a commit so released APKs keep working when branches change.
 * When updating a runtime, update the commit, version, hashes and sizes together.
 */
object RuntimeCatalog {
    const val BASE_URL = "https://raw.githubusercontent.com/KedaMC/RayLauncher/67be39cd5e6519fbbd883199261c7f9cbb14af32/runtimes"

    val releases: Map<Jre, RuntimeRelease> = mapOf(
        Jre.JRE_8 to RuntimeRelease(
            "2",
            mapOf(
                "bin-arm.tar.xz" to RuntimePackage("b6f33526b91d037e996226daa9db94419d52c459", 3375000L),
                "bin-arm64.tar.xz" to RuntimePackage("ffa036ee0b48271a4e0756d2be60989b77031f76", 4347796L),
                "universal.tar.xz" to RuntimePackage("23cd14560abdfc56a951dbe4c0ab75012f199016", 9540292L)
            )
        ),
        Jre.JRE_17 to RuntimeRelease(
            "2",
            mapOf(
                "bin-arm.tar.xz" to RuntimePackage("53d73c62ff15a238a1afb5531e3f6bb1a2568475", 5558048L),
                "bin-arm64.tar.xz" to RuntimePackage("dd2d348b254600ac0452207d28e815de3f82dcf8", 4640448L),
                "universal.tar.xz" to RuntimePackage("292aa9801fd8969dfefb2fe91d723aa87e0bf088", 17070632L)
            )
        ),
        Jre.JRE_21 to RuntimeRelease(
            "2",
            mapOf(
                "bin-arm.tar.xz" to RuntimePackage("509a2711c44ab6962d9c64cd22d401b4e067d12b", 5710624L),
                "bin-arm64.tar.xz" to RuntimePackage("f36209bfcc8bf939aa05eaab71858d14a9be1a2a", 6847128L),
                "universal.tar.xz" to RuntimePackage("6566d32b34211ebd75810c140d740966a3a76767", 18974588L)
            )
        ),
        Jre.JRE_25 to RuntimeRelease(
            "2",
            mapOf(
                "bin-arm.tar.xz" to RuntimePackage("c092beee502d2518f1485880384996eae12cf55c", 4216116L),
                "bin-arm64.tar.xz" to RuntimePackage("498bbfdaee5d425b09402ba8b4dc48b93da69f4f", 5453168L),
                "universal.tar.xz" to RuntimePackage("8e3b22d9fbdb56a39fab46cea5cf33dbb56db4da", 32814644L)
            )
        )
    )
}
