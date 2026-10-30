package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nightmare Shepherd
 * {2}{B}{B}
 * Enchantment Creature — Demon
 * 4/4
 * Flying
 * Whenever another nontoken creature you control dies, you may exile it. If you do, create a token
 * that's a copy of that creature, except it's 1/1 and it's a Nightmare in addition to its other types.
 *
 * Kinzu of the Bleak Coven's shape without the life payment: [Effects.May] over an
 * [Effects.IfYouDo] whose action exiles the dead card, so nothing is made if the card already left
 * the graveyard. The copy reads the creature's last-known copiable values from its departure
 * snapshot (first ruling), with 1/1 and the added Nightmare subtype as copy exceptions — copiable,
 * so a copy of the token is a 1/1 Nightmare too.
 */
val NightmareShepherd = card("Nightmare Shepherd") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment Creature — Demon"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "Whenever another nontoken creature you control dies, you may exile it. If you do, create a token " +
        "that's a copy of that creature, except it's 1/1 and it's a Nightmare in addition to its other types."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl().nontoken()).dies()
        effect = Effects.May(
            Effects.IfYouDo(
                action = Effects.Pipeline {
                    exile(gather(CardSource.Entity(EffectTarget.TriggeringEntity)))
                },
                then = Effects.CreateTokenCopyOfTarget(
                    EffectTarget.TriggeringEntity,
                    exceptions = CopyExceptions(
                        powerOverride = 1,
                        toughnessOverride = 1,
                        addedSubtypes = setOf(Subtype.NIGHTMARE),
                    ),
                ),
            )
        )
        description = "Whenever another nontoken creature you control dies, you may exile it. If you do, " +
            "create a token that's a copy of that creature, except it's 1/1 and it's a Nightmare in " +
            "addition to its other types."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "108"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4a46eec7-bb79-441c-959f-eea599e5c341.jpg?1783931562"
        ruling("2020-01-24", "The token copies the creature as it last existed on the battlefield before it died, not as it existed in the graveyard before it was exiled.")
        ruling("2020-01-24", "The token copies exactly what was printed on the original creature and nothing else (unless that creature is copying something else; see below). It doesn't copy whether that creature was tapped or untapped, whether it had any counters on it or Auras or Equipment attached to it, or any non-copy effects that had changed its power, toughness, types, color, or so on.")
        ruling("2020-01-24", "If the copied creature was copying something else, then the token enters the battlefield as whatever that creature copied.")
        ruling("2020-01-24", "If the copied creature had {X} in its mana cost, X is considered to be 0.")
        ruling("2020-01-24", "If the token is a copy of a creature whose power and toughness are defined by an ability (usually printed as */* or similar), the token doesn't copy the ability that defines its power and toughness. It remains a 1/1 creature.")
        ruling("2020-01-24", "If something becomes a copy of the token, the copy is also a 1/1 and a Nightmare.")
    }
}
