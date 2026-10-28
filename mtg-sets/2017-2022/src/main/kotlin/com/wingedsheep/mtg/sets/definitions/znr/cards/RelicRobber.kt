package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Relic Robber — Zendikar Rising #153
 * {2}{R} · Creature — Goblin Rogue · 2/2
 *
 * Haste
 * Whenever this creature deals combat damage to a player, that player creates a 0/1 colorless
 * Goblin Construct artifact creature token with "This token can't block" and "At the beginning of
 * your upkeep, this token deals 1 damage to you."
 *
 * The damaged player ([Player.TriggeringPlayer]) controls the token, so the token's own "your
 * upkeep" trigger fires on *their* upkeep and "you" is them — the token's controller.
 */
val RelicRobber = card("Relic Robber") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Rogue"
    power = 2
    toughness = 2
    oracleText = "Haste\nWhenever this creature deals combat damage to a player, that player creates a 0/1 " +
        "colorless Goblin Construct artifact creature token with \"This token can't block\" and \"At the " +
        "beginning of your upkeep, this token deals 1 damage to you.\""

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.CreateToken(
            power = 0,
            toughness = 1,
            creatureTypes = setOf("Goblin", "Construct"),
            artifactToken = true,
            controller = EffectTarget.PlayerRef(Player.TriggeringPlayer),
            staticAbilities = listOf(CantBlock()),
            triggeredAbilities = listOf(
                grantedTriggeredAbility {
                    trigger = Triggers.you.beginningOf(Step.UPKEEP)
                    effect = Effects.DealDamage(1, EffectTarget.Controller)
                    description = "At the beginning of your upkeep, this token deals 1 damage to you."
                }
            ),
            imageUri = "https://cards.scryfall.io/normal/front/b/2/b24a63e2-c2b3-43f8-a15a-68886bec7d60.jpg?1783929495",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "153"
        artist = "Slawomir Maniak"
        flavorText = "\"Here, you take it!\""
        imageUri = "https://cards.scryfall.io/normal/front/4/5/4540205c-eee8-4db3-8757-710de874b313.jpg?1783929355"
    }
}
