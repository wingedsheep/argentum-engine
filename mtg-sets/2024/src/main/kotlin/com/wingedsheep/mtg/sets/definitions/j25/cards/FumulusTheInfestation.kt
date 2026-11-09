package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fumulus, the Infestation — Foundations Jumpstart #42
 * {3}{B} · Legendary Creature — Vampire Insect · 2/2
 *
 * Flying, deathtouch
 * Whenever a player sacrifices a nontoken creature, create a 1/1 black Insect creature token with
 * flying.
 * Whenever an Insect, Leech, Slug, or Worm you control attacks, defending player loses 1 life and
 * you gain 1 life.
 *
 * The sacrifice trigger is `Triggers.anyPlayer.sacrifices(...)` — any player's sacrifice, once per
 * nontoken creature (Mayhem Devil's scope with Tergrid's nontoken filter). A Fumulus sacrificed
 * alongside or as the fodder still sees it via the sacrifice look-back (per the ruling). The attack
 * trigger is per attacking creature (Brutal Hordechief's drain), Fumulus included as an Insect.
 */
val FumulusTheInfestation = card("Fumulus, the Infestation") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Vampire Insect"
    power = 2
    toughness = 2
    oracleText = "Flying, deathtouch\n" +
        "Whenever a player sacrifices a nontoken creature, create a 1/1 black Insect creature token " +
        "with flying.\n" +
        "Whenever an Insect, Leech, Slug, or Worm you control attacks, defending player loses 1 life " +
        "and you gain 1 life."

    keywords(Keyword.FLYING, Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.anyPlayer.sacrifices(GameObjectFilter.Creature.nontoken())
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Insect"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/2/1/2172a126-2ea6-4b5c-84c2-7879c1982a3a.jpg?1783905793"
        )
        description = "Whenever a player sacrifices a nontoken creature, create a 1/1 black Insect " +
            "creature token with flying."
    }

    triggeredAbility {
        trigger = Triggers.a(
            GameObjectFilter.Creature.withAnySubtype("Insect", "Leech", "Slug", "Worm").youControl()
        ).attacks()
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.DefendingPlayer)) then
            Effects.GainLife(1)
        description = "Whenever an Insect, Leech, Slug, or Worm you control attacks, defending player " +
            "loses 1 life and you gain 1 life."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "42"
        artist = "Takotto"
        imageUri = "https://cards.scryfall.io/normal/front/e/e/ee5e47c2-7648-4218-b42c-ed9650e12914.jpg?1783908857"
        ruling("2024-11-08", "If a nontoken creature is sacrificed to pay a cost of a spell or ability, Fumulus's second ability will resolve before that spell or ability. Conversely, if a nontoken creature is sacrificed during the resolution of a spell or ability, that spell or ability will finish resolving before Fumulus's second ability is put onto the stack.")
        ruling("2024-11-08", "If you sacrifice Fumulus, its second ability will still trigger.")
    }
}
