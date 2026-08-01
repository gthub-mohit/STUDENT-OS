package com.studentos.feature.coding.domain.usecase

import com.studentos.feature.coding.domain.repository.DsaRepository
import javax.inject.Inject

class AddDsaCategoryUseCase @Inject constructor(
    private val dsaRepository: DsaRepository
) {
    suspend operator fun invoke(name: String, sortOrder: Int = 0): Long {
        require(name.isNotBlank()) { "Category name cannot be blank" }
        return dsaRepository.addCategory(name.trim(), sortOrder)
    }
}
