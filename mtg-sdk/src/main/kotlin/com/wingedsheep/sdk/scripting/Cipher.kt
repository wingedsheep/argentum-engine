package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.conditions.EntityMatches
import com.wingedsheep.sdk.scripting.effects.CastFromCollectionWithoutPayingCostEffect
import com.wingedsheep.sdk.scripting.effects.ChoiceLabels
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.CopyCardIntoCollectionEffect
import com.wingedsheep.sdk.scripting.effects.EncodeOnCreatureEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Cipher (CR 702.99) as pure data — the two abilities the keyword stands for, which no cipher card
 * prints as separate lines.
 *
 * > "Cipher" means "If this spell is represented by a card, you may exile this card encoded on a
 * > creature you control" and "For as long as this card is encoded on that creature, that creature
 * > has 'Whenever this creature deals combat damage to a player, you may copy the encoded card and
 * > you may cast the copy without paying its mana cost.'" (CR 702.99a)
 *
 *  - **Encoding** — [encodeStep], which the engine appends to a resolving card's effect when the
 *    card has [com.wingedsheep.sdk.core.Keyword.CIPHER]: the controller picks up to one creature
 *    they control as the spell resolves (no targeting — the rulings), after the spell's other
 *    effects, and [EncodeOnCreatureEffect] records the pick; the spell then leaves the stack straight
 *    to exile, encoded.
 *  - **The granted trigger** — [encodedCardTrigger], granted by the engine to the chosen creature
 *    for as long as the card stays encoded on it (CR 702.99c: the card stays exiled and the
 *    creature stays on the battlefield; a control change or the creature ceasing to be a creature
 *    doesn't end it). The creature's controller controls the trigger, so a stolen creature casts
 *    the copy for its new controller. Each encoded card is its own trigger, keyed by the card.
 *
 * The copy is made in exile and cast through the ordinary
 * [CopyCardIntoCollectionEffect] → [CastFromCollectionWithoutPayingCostEffect] pipeline, the shape
 * Paradigm and Isochron Scepter use. A copy that isn't cast ceases to exist at the next
 * state-based-action check (CR 707.10a), so the printed "you may copy … and you may cast" collapses
 * into one decision: declining either is indistinguishable.
 */
object Cipher {

    /** Pipeline collection holding the creatures the controller may encode the card on. */
    const val CANDIDATES: String = "cipher_candidates"

    /** Pipeline collection holding the chosen creature (empty when the controller declines). */
    const val CHOSEN: String = "cipher_chosen"

    /** Pipeline collection the trigger hands the fresh copy to the free-cast step through. */
    const val COPY_COLLECTION: String = "cipher_copy"

    /** Ability id prefix; the encoded card's id is appended so several encoded cards differ. */
    private const val ABILITY_ID_PREFIX = "cipher"

    /** CR 702.99a, first ability — "you may exile this card encoded on a creature you control". */
    val encodeStep: Effect = CompositeEffect(
        listOf(
            GatherCardsEffect(
                source = CardSource.ControlledPermanents(filter = GameObjectFilter.Creature),
                storeAs = CANDIDATES,
            ),
            SelectFromCollectionEffect(
                from = CANDIDATES,
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                storeSelected = CHOSEN,
                prompt = "Cipher — you may encode this card on a creature you control",
                selectedLabel = "Encode",
                useTargetingUI = true,
            ),
            EncodeOnCreatureEffect(from = CHOSEN),
        ),
        descriptionOverride = "you may exile this card encoded on a creature you control",
    )

    /**
     * CR 702.99a, second ability — the trigger the creature has while [cardId] is encoded on it.
     *
     * The copy is made only while [cardId] is still in exile when the trigger resolves: a card that
     * has left exile is no longer the encoded card (CR 702.99c), so there is nothing to copy.
     *
     * @param cardId the encoded card in exile.
     * @param cardName its name, for the ability text and the badge on the creature.
     */
    fun encodedCardTrigger(cardId: EntityId, cardName: String): TriggeredAbility = TriggeredAbility(
        id = AbilityId("${ABILITY_ID_PREFIX}_${cardId.value}"),
        trigger = EventPattern.DealsDamageEvent(
            damageType = DamageType.Combat,
            recipient = Recipient.AnyPlayer,
        ),
        binding = TriggerBinding.SELF,
        activeZones = setOf(Zone.BATTLEFIELD),
        effect = Effects.If(
            EntityMatches(
                EffectTarget.SpecificEntity(cardId),
                GameObjectFilter.Any.currentlyIn(Zone.EXILE),
            ),
            Effects.May(
                CompositeEffect(
                    listOf(
                        CopyCardIntoCollectionEffect(
                            source = EffectTarget.SpecificEntity(cardId),
                            storeAs = COPY_COLLECTION,
                        ),
                        CastFromCollectionWithoutPayingCostEffect(from = COPY_COLLECTION),
                    )
                ),
                descriptionOverride = "copy $cardName and cast the copy without paying its mana cost",
                choiceLabels = ChoiceLabels(
                    prompt = "Cipher — cast a copy of $cardName without paying its mana cost?",
                    yes = "Cast copy",
                    no = "Don't cast",
                ),
            ),
        ),
        descriptionOverride = "Cipher ($cardName) — Whenever this creature deals combat damage to a " +
            "player, you may copy the encoded card and you may cast the copy without paying its " +
            "mana cost.",
    )
}
