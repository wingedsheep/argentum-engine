package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.ninjutsu
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.EnteringAttackDefender
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Thousand-Faced Shadow — Kamigawa: Neon Dynasty #86 (canonical printing)
 * {U} · Creature — Human Ninja · 1/1
 *
 * Ninjutsu {2}{U}{U}
 * Flying
 * When this creature enters from your hand, if it's attacking, create a token that's a copy of
 * another target attacking creature. The token enters tapped and attacking.
 *
 * "Enters from your hand" is a hand → battlefield zone change: ninjutsu puts the card onto the
 * battlefield straight from hand, while a cast Shadow enters from the stack and never triggers.
 * "If it's attacking" is an intervening if (CR 603.4), checked again on resolution. The token's
 * controller picks what it attacks as it enters (ruling 2022-02-18), so it needn't attack the
 * same player or planeswalker the Shadow does.
 */
val ThousandFacedShadow = card("Thousand-Faced Shadow") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Ninja"
    power = 1
    toughness = 1
    oracleText = "Ninjutsu {2}{U}{U} ({2}{U}{U}, Return an unblocked attacker you control to hand: Put " +
        "this card onto the battlefield from your hand tapped and attacking.)\n" +
        "Flying\n" +
        "When this creature enters from your hand, if it's attacking, create a token that's a copy of " +
        "another target attacking creature. The token enters tapped and attacking."

    ninjutsu("{2}{U}{U}")
    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters(from = Zone.HAND)
        interveningIf = Conditions.SourceIsAttacking
        val original = target(TargetFilter(GameObjectFilter.Creature.attacking()).other())
        effect = Effects.CreateTokenCopyOfTarget(
            target = original,
            tapped = true,
            attacking = true,
            attackDefender = EnteringAttackDefender.CONTROLLER_CHOOSES,
        )
        description = "When this creature enters from your hand, if it's attacking, create a token that's " +
            "a copy of another target attacking creature. The token enters tapped and attacking."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "86"
        artist = "Ekaterina Burmak"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/89a698a5-f4ac-4b04-96dc-32e1eeef6ac7.jpg?1783923891"
        ruling("2022-02-18", "You choose which opponent or opposing planeswalker the token is attacking as you put it onto the battlefield. It doesn't have to be the same player or planeswalker Thousand-Faced Shadow is attacking.")
        ruling("2022-02-18", "Although the token is attacking, it was never declared as an attacking creature (for purposes of abilities that trigger whenever a creature attacks, for example).")
        ruling("2022-02-18", "The token copies exactly what was printed on the original creature and nothing else (unless that permanent is copying something else or is a token). It doesn't copy counters, Auras, Equipment, or non-copy effects. If the token isn't a creature as it enters the battlefield, it won't be attacking.")
    }
}
