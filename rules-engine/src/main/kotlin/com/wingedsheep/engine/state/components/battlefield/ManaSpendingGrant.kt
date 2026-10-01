package com.wingedsheep.engine.state.components.battlefield

import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.SpendManaAsColor
import kotlinx.serialization.Serializable

@Serializable
data class ManaSpendingGrant(val permission: SpendManaAsColor, val conditions: List<Condition> = emptyList())

