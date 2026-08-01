package com.studentos.feature.coding.domain.usecase

import com.studentos.feature.coding.domain.repository.DsaRepository
import javax.inject.Inject

class DeleteDsaCategoryUseCase @Inject constructor(
    private val dsaRepository: DsaRepository
) {
    suspend operator fun invoke(categoryId: Long) {
        require(categoryId > 0) { "Invalid category ID" }
        dsaRepository.deleteCategory(categoryId)
    }
}
