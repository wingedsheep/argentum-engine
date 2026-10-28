package com.wingedsheep.sdk.scripting.effects

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Cipher's encoding step (CR 702.99a): "you may exile this card encoded on a creature you
 * control". Exiles the resolving spell card straight from the stack (it never reaches the
 * graveyard) and encodes it on the creature in collection [from]; that creature then has the
 * cipher trigger ([com.wingedsheep.sdk.scripting.Cipher.encodedCardTrigger]) for as long as the
 * card stays encoded (CR 702.99c).
 *
 * SDK-internal: no card authors it. The engine appends
 * [com.wingedsheep.sdk.scripting.Cipher.encodeStep] — gather, an optional pick of one creature,
 * then this — to a resolving card with [com.wingedsheep.sdk.core.Keyword.CIPHER].
 *
 * Closest existing types, and why neither takes the axis:
 *  - [MakePlottedEffect] marks *already-exiled* cards and hands out a cast permission; cipher moves
 *    the resolving spell itself off the stack and grants an ability to a different object.
 *  - [MoveCollectionEffect] moves a gathered collection; the card moved here is the spell (never in
 *    the collection), and the relationship it leaves behind is to the *chosen creature*, not to the
 *    effect's source, which is what `linkToSource` records.
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
