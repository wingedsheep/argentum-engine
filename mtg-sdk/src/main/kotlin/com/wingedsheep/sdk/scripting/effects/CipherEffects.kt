package com.wingedsheep.sdk.scripting.effects

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Cipher's encoding step (CR 702.99a): "you may exile this card encoded on a creature you
 * control". Records the creature in collection [from] on the resolving spell; as the spell then
 * leaves the stack, the engine's resolution path sends the card straight to exile (it never
 * reaches the graveyard) and encodes it there, and the creature has the cipher trigger
 * ([com.wingedsheep.sdk.scripting.Cipher.encodedCardTrigger]) for as long as the card stays
 * encoded (CR 702.99c).
 *
 * SDK-internal: no card authors it. The engine appends
 * [com.wingedsheep.sdk.scripting.Cipher.encodeStep] — gather, an optional pick of one creature,
 * then this — to a resolving card with [com.wingedsheep.sdk.core.Keyword.CIPHER].
 *
 * Closest existing types, and why neither takes the axis — both also mark a spell for its
 * resolution destination:
 *  - [MarkSpellExileOnResolveEffect] marks a *target* spell to be exiled (then plotted, or returned) *instead of
 *    going to the graveyard*, and hands out a cast permission; cipher's exile is the resolving
 *    spell's own last instruction (not a graveyard replacement, so rebound and graveyard riders
 *    don't apply) and leaves a link to a chosen creature that gains an ability.
 *  - [MarkSpellExileWithCountersEffect] is the same graveyard replacement with counters on the
 *    exiled card; it carries no relationship to another object.
 *
 * Does nothing when [from] is empty (the controller declined), when the resolving spell isn't
 * represented by a card (a copy — CR 702.99a "If this spell is represented by a card"), or when
 * the chosen creature is no longer a creature on the battlefield under the spell's controller.
 *
 * @property from Pipeline collection holding the chosen creature (zero or one entity).
 */
@SerialName("EncodeOnCreature")
@Serializable
data class EncodeOnCreatureEffect(
    val from: String
) : Effect {
    override val description: String = "Exile this card encoded on that creature"
}
