package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Moonstone, Harsh Mistress — Marvel Super Heroes #107
 * {3}{B} · Legendary Creature — Human Doctor Villain · 2/4
 *
 * Flying
 * Whenever you discard a card, you may exile that card from your graveyard. If you do, until the
 * end of your next turn, you may play that card.
 *
 * `Triggers.you.discards()` is the per-card (non-batch) discard trigger, so discarding three cards in
 * one resolution fires it three times and each firing binds *its* card as the triggering entity
 * (CR 400.7e — the trigger can find the object the discarded card became in the graveyard). That
 * makes [CardSource.Entity] "that card", feeding the Norin, Swift Survivalist
 * gather → exile → grant pipeline wrapped in a [Effects.May] for the optional "you may exile".
 *
 * The permission is granted only on the exiled card, and playing it still costs its mana. Its
 * expiry is [MayPlayExpiry.UntilEndOfNextTurn] — `UntilControllerStep(CLEANUP,
 * includeCurrentTurn = false)`, which never expires during the turn the trigger resolved, so a
 * discard on your own turn still leaves the card playable through the whole of your next turn.
 *
 * Known gap: the gather is zone-agnostic, so if something moves the discarded card out of your
 * graveyard in response to this trigger, the ability would still exile it from wherever it ended
 * up instead of doing nothing. Restricting a gather to a source zone has no SDK vocabulary today.
 */
val MoonstoneHarshMistress = card("Moonstone, Harsh Mistress") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Doctor Villain"
    power = 2
    toughness = 4
    oracleText = "Flying\n" +
        "Whenever you discard a card, you may exile that card from your graveyard. If you do, " +
        "until the end of your next turn, you may play that card."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.discards()
        effect = Effects.May(
            Effects.Pipeline {
                val moonstoneDiscarded = gather(CardSource.Entity(EffectTarget.TriggeringEntity))
                exile(moonstoneDiscarded)
                run(Effects.GrantMayPlayFromExile(
                    moonstoneDiscarded,
                    MayPlayExpiry.UntilEndOfNextTurn
                ))
            },
            descriptionOverride = "Exile that card from your graveyard? You may play it until the " +
                "end of your next turn."
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "107"
        artist = "Grace Zhu"
        flavorText = "\"Let me show you the dark side of the moon.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1a1b2bd0-e17d-4b34-a8ee-7c913a6bc945.jpg?1783902940"
    }
}
