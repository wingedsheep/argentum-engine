package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.ActivatedAbility

/**
 * A permanent's *own* activated abilities: its definition's (at [classLevel]) plus any a copy
 * exception added to its copiable values ([CardComponent.copyActivatedAbilities], CR 707.9a).
 *
 * "Own" is the distinction the activation paths draw against granted abilities — both halves are
 * suppressed by face-down status and by losing all abilities, which a layer-six grant is not.
 * Every reader that asks "what can this permanent itself activate?" goes through here so a copy
 * exception is never offered by the enumerator and rejected by the handler, or vice versa.
 */
fun ownActivatedAbilities(
    card: CardComponent,
    cardDef: CardDefinition?,
    classLevel: Int?,
): List<ActivatedAbility> {
    val printed = cardDef?.script?.effectiveActivatedAbilities(classLevel).orEmpty()
    return if (card.copyActivatedAbilities.isEmpty()) printed else printed + card.copyActivatedAbilities
}
