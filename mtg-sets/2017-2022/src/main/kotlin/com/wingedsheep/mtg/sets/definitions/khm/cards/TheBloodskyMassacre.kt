package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.effects.ManaExpiry
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Bloodsky Massacre — Kaldheim #207
 * {1}{B}{R} · Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Create a 2/3 red Demon Berserker creature token with menace.
 * II — Whenever a Berserker attacks this turn, you draw a card and you lose 1 life.
 * III — Add {R} for each Berserker you control. Until end of turn, you don't lose this mana as
 * steps and phases end.
 *
 * Chapter II installs a turn-bounded delayed trigger over *any* attacking Berserker — the text
 * doesn't say "you control", so an opponent's Berserker attacking this turn also draws you a card.
 * Chapter III is a triggered ability on the stack (per the ruling, not a mana ability) that counts
 * the Berserker permanents you control on resolution and adds kept-until-end-of-turn mana.
 */
val TheBloodskyMassacre = card("The Bloodsky Massacre") {
    manaCost = "{1}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Create a 2/3 red Demon Berserker creature token with menace.\n" +
        "II — Whenever a Berserker attacks this turn, you draw a card and you lose 1 life.\n" +
        "III — Add {R} for each Berserker you control. Until end of turn, you don't lose this mana as " +
        "steps and phases end."

    sagaChapter(1) {
        effect = Effects.CreateToken(
            power = 2,
            toughness = 3,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Demon", "Berserker"),
            keywords = setOf(Keyword.MENACE),
            imageUri = "https://cards.scryfall.io/normal/front/c/5/c58def55-f2ca-4c41-b891-c32e5220fc66.jpg?1783928079"
        )
    }

    sagaChapter(2) {
        effect = Effects.CreateDelayedTrigger(
            trigger = Triggers.a(GameObjectFilter.Creature.withSubtype(Subtype.BERSERKER)).attacks(),
            fireOnce = false,
            expiry = DelayedTriggerExpiry.EndOfTurn
        ) {
            effect = Effects.DrawCards(1) then Effects.LoseLife(1, EffectTarget.Controller)
        }
    }

    sagaChapter(3) {
        effect = Effects.AddMana(
            Color.RED,
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Any.withSubtype(Subtype.BERSERKER)).count(),
            expiry = ManaExpiry.KEPT_UNTIL_END_OF_TURN
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "207"
        artist = "Livia Prima"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/669975ed-0501-4b48-988d-29c1215179a4.jpg?1783928198"
        ruling(
            "2021-02-05",
            "The chapter III ability isn't a mana ability. It uses the stack and can be responded to. Use " +
                "the number of Berserkers you control as the ability resolves to determine how much {R} to add."
        )
    }
}
