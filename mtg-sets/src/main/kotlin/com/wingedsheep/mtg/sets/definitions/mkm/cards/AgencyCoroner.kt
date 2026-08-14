package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect

/**
 * Agency Coroner — Murders at Karlov Manor #75
 * {4}{B} · Creature — Ogre Cleric · 3/6
 *
 * {2}{B}, Sacrifice another creature: Draw a card. If the sacrificed creature was suspected,
 * draw two cards instead.
 *
 * The black common's sacrifice outlet, paying double when the body was already a suspect — MKM's
 * self-suspecting attackers (Frantic Scapegoat, Barbed Servitor) and the set's many "suspect target
 * creature" riders all feed it.
 *
 * "Was suspected" is *last-known information* (CR 608.2h), not a board read. The suspected
 * designation (CR 701.60a) is a floating effect keyed on the entity, so it dies with the creature
 * the instant the cost is paid — long before the ability resolves. `Conditions.SacrificedWasSuspected`
 * therefore reads the flag frozen onto the cost-time `EntitySnapshot`, the same mechanism
 * `SacrificedWasLegendary` (Nasty End) uses for supertypes.
 *
 * The branch is a [ConditionalEffect] over "draw two" / "draw one", never "draw one, then draw one
 * more if …". The printed word is *instead*: this is a single draw event of one size or the other,
 * which matters to anything watching draws.
 *
 * `Costs.SacrificeAnother` excludes the Coroner itself, so it can't eat itself for value — and with
 * no other creature on the battlefield the ability simply isn't activatable.
 */
val AgencyCoroner = card("Agency Coroner") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Ogre Cleric"
    power = 3
    toughness = 6
    oracleText = "{2}{B}, Sacrifice another creature: Draw a card. If the sacrificed creature was " +
        "suspected, draw two cards instead."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{B}"),
            Costs.SacrificeAnother(GameObjectFilter.Creature)
        )
        effect = ConditionalEffect(
            condition = Conditions.SacrificedWasSuspected,
            effect = Effects.DrawCards(2),
            elseEffect = Effects.DrawCards(1)
        )
        description = "Draw a card. If the sacrificed creature was suspected, draw two cards instead."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "75"
        artist = "Uriah Voth"
        flavorText = "\"Well, cause of death seems pretty straightforward, but what else can you " +
            "tell me, friend?\""
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d63f2c23-e877-42e3-9362-5d003a173c6d.jpg?1783912901"

        ruling(
            "2024-02-02",
            "When an effect suspects a creature, it becomes suspected. It gains menace and \"This " +
                "creature can't block\" for as long as it's suspected. It stays suspected until it " +
                "leaves the battlefield or another effect causes it to no longer be suspected."
        )
        ruling(
            "2024-02-02",
            "If a suspected creature loses all abilities, it will lose menace and \"This creature " +
                "can't block\", but it won't stop being suspected."
        )
    }
}
