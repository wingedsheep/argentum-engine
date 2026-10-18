package com.wingedsheep.engine.mechanics.layers

import com.wingedsheep.engine.mechanics.targeting.ColorProtection
import com.wingedsheep.engine.mechanics.targeting.HexproofFromRules
import com.wingedsheep.engine.mechanics.targeting.SourceKindProtection
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.components.identity.HexproofFromComponent
import com.wingedsheep.engine.state.components.identity.ProtectionComponent

/**
 * The projected keyword spellings of an object's *printed* protection and hexproof-from abilities
 * (`PROTECTION_FROM_*`, `HEXPROOF_FROM_*`), read off the keyword-derived [ProtectionComponent] /
 * [HexproofFromComponent] decorations. One reader for the projector's starting keywords and for
 * the statics that copy another card's printed qualities (Cairn Wanderer, Eater of Virtue), so the
 * two can never spell a quality differently.
 */
internal object PrintedKeywordQualities {

    fun protections(container: ComponentContainer, into: MutableCollection<String>) {
        val protection = container.get<ProtectionComponent>() ?: return
        protection.colors.forEach { into.add("PROTECTION_FROM_${it.name}") }
        protection.subtypes.forEach { into.add("PROTECTION_FROM_SUBTYPE_${it.uppercase()}") }
        protection.supertypes.forEach { into.add("PROTECTION_FROM_SUPERTYPE_${it.uppercase()}") }
        protection.cardTypes.forEach { into.add("PROTECTION_FROM_CARDTYPE_$it") }
        protection.sourceKinds.forEach { into.add(SourceKindProtection.protectionKeyword(it)) }
        if (protection.multicolored) into.add(ColorProtection.PROTECTION_FROM_MULTICOLORED)
    }

    fun hexproofFroms(container: ComponentContainer, into: MutableCollection<String>) {
        val hexproof = container.get<HexproofFromComponent>() ?: return
        hexproof.colors.forEach { into.add("HEXPROOF_FROM_${it.name}") }
        hexproof.cardTypes.forEach { into.add("HEXPROOF_FROM_CARDTYPE_$it") }
        hexproof.nonColors.forEach { into.add(HexproofFromRules.nonColorKeyword(it)) }
        hexproof.sourceKinds.forEach { into.add(SourceKindProtection.hexproofKeyword(it)) }
        if (hexproof.monocolored) into.add(HexproofFromRules.MONOCOLORED)
        if (hexproof.multicolored) into.add(HexproofFromRules.MULTICOLORED)
    }
}
