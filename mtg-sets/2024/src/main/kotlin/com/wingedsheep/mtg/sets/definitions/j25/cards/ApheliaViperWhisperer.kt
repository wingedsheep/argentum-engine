package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Aphelia, Viper Whisperer — Foundations Jumpstart #40
 * {1}{B} · Legendary Creature — Gorgon Assassin · 1/3
 *
 * Deathtouch
 * Whenever Aphelia attacks, you may pay {1}{B/G}. If you do, create a 1/1 black Snake creature
 * token with deathtouch.
 * {4}{B}: Until end of turn, whenever one or more Gorgons and/or Snakes you control deal combat
 * damage to a player, that player loses half their life, rounded up.
 *
 * The activated ability creates a floating "until end of turn" triggered ability owned by no
 * permanent ([Effects.CreateGlobalTriggeredAbility] with [Duration.EndOfTurn], The Raven's
 * Warning's shape), so it keeps working if Aphelia leaves the battlefield and each activation
 * adds its own copy. Its trigger is the batched `oneOrMore(...).dealCombatDamageToAPlayer()`:
 * once per player hit however many Gorgons and Snakes connected, with "that player" bound as the
 * triggering player. The half is computed on resolution, after combat damage has already been
 * dealt (ruling).
 */
val ApheliaViperWhisperer = card("Aphelia, Viper Whisperer") {
    manaCost = "{1}{B}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Gorgon Assassin"
    power = 1
    toughness = 3
    oracleText = "Deathtouch\n" +
        "Whenever Aphelia attacks, you may pay {1}{B/G}. If you do, create a 1/1 black Snake creature " +
        "token with deathtouch.\n" +
        "{4}{B}: Until end of turn, whenever one or more Gorgons and/or Snakes you control deal combat " +
        "damage to a player, that player loses half their life, rounded up."

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.MayPay(
            cost = ManaCost.parse("{1}{B/G}"),
            then = Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.BLACK),
                creatureTypes = setOf("Snake"),
                keywords = setOf(Keyword.DEATHTOUCH),
                imageUri = "https://cards.scryfall.io/normal/front/6/2/62340bbb-cff0-48c1-8854-235340e80477.jpg?1783903573"
            )
        )
    }

    activatedAbility {
        cost = Costs.Mana("{4}{B}")
        effect = Effects.CreateGlobalTriggeredAbility(
            duration = Duration.EndOfTurn,
            ability = TriggeredAbility.create(
                // "you control" is the event's own (OneOrMoreDealCombatDamageToPlayerEvent reads
                // "matching sourceFilter you control"), so the filter carries no controller predicate.
                trigger = Triggers.oneOrMore(GameObjectFilter.Creature.withAnySubtype("Gorgon", "Snake"))
                    .dealCombatDamageToAPlayer(),
                effect = Effects.LoseHalfLife(
                    roundUp = true,
                    target = EffectTarget.PlayerRef(Player.TriggeringPlayer),
                    lifePlayer = Player.TriggeringPlayer
                )
            ),
            descriptionOverride = "Until end of turn, whenever one or more Gorgons and/or Snakes you control " +
                "deal combat damage to a player, that player loses half their life, rounded up."
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "40"
        artist = "Fuzichoco"
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1a0867be-a861-4fb4-b8ff-cb2966193755.jpg?1783908857"
        ruling(
            "2024-11-08",
            "The triggered ability created by Aphelia's last ability triggers and resolves after combat damage " +
                "is dealt. For example, if Gorgons and/or Snakes you control deal a total of 3 combat damage to a " +
                "player with 12 life, combat damage will reduce that player's life total to 9. Then the triggered " +
                "ability will cause the player to lose 5 life, leaving the player at 4."
        )
    }
}
