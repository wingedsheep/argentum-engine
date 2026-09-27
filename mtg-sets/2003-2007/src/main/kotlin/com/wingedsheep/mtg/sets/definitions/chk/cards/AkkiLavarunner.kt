package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyDamageAmount
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Akki Lavarunner // Tok-Tok, Volcano Born (Champions of Kamigawa #153) — a flip card (CR 710).
 *
 * Akki Lavarunner {3}{R} — Creature — Goblin Warrior 1/1
 * "Haste. Whenever this creature deals damage to an opponent, flip it."
 *
 * Tok-Tok, Volcano Born — Legendary Creature — Goblin Shaman 2/2
 * "Protection from red. If a red source would deal damage to a player, it deals that much damage
 * plus 1 to that player instead."
 *
 * Tok-Tok's replacement is symmetric: it applies to any red source, whoever controls it, and to
 * damage dealt to any player — including Tok-Tok's controller.
 */
private val AkkiLavarunnerUpright = card("Akki Lavarunner") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Warrior"
    oracleText = "Haste\nWhenever this creature deals damage to an opponent, flip it."
    power = 1
    toughness = 1

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.dealsDamage(Recipient.Opponent)
        effect = Effects.Flip()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "153"
        artist = "Matt Cavotta"
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6ee6cd34-c117-4d7e-97d1-8f8464bfaac8.jpg?1783944304"
    }
}

private val TokTokVolcanoBorn = card("Tok-Tok, Volcano Born") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Goblin Shaman"
    oracleText = "Protection from red\nIf a red source would deal damage to a player, it deals that much " +
        "damage plus 1 to that player instead."
    power = 2
    toughness = 2

    keywordAbility(KeywordAbility.Protection(ProtectionScope.Color(Color.RED)))

    replacementEffect(
        ModifyDamageAmount(
            modifier = 1,
            appliesTo = EventPattern.DamageEvent(
                source = GameObjectFilter.Any.withColor(Color.RED),
                recipient = Recipient.AnyPlayer,
            ),
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "153"
        artist = "Matt Cavotta"
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6ee6cd34-c117-4d7e-97d1-8f8464bfaac8.jpg?1783944304"
    }
}

val AkkiLavarunner: CardDefinition = CardDefinition.flipCard(
    unflipped = AkkiLavarunnerUpright,
    flipped = TokTokVolcanoBorn,
)
