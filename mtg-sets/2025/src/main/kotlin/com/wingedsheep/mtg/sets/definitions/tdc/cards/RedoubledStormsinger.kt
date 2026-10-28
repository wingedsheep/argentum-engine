package com.wingedsheep.mtg.sets.definitions.tdc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.EnteringAttackDefender
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Redoubled Stormsinger — Tarkir: Dragonstorm Commander #37
 * {2}{R} · Creature — Orc Wizard · 3/3
 *
 * First strike
 * Whenever this creature attacks, for each creature token you control that entered this turn,
 * create a tapped and attacking token that's a copy of that token. At the beginning of the next
 * end step, sacrifice those tokens.
 *
 * The Ocelot Pride shape: [Effects.ForEachInGroup] over creature tokens you control that entered
 * this turn (snapshotted before any copy is made, so the new copies aren't re-iterated), each
 * iteration making one tapped-and-attacking [Effects.CreateTokenCopyOfTarget] of the iterated
 * token. Per the rulings, each copy's controller picks what it attacks as it enters
 * ([EnteringAttackDefender.CONTROLLER_CHOOSES]); `sacrificeAtStep` arms the delayed sacrifice of
 * exactly the tokens this ability created.
 */
val RedoubledStormsinger = card("Redoubled Stormsinger") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Orc Wizard"
    power = 3
    toughness = 3
    oracleText = "First strike\n" +
        "Whenever this creature attacks, for each creature token you control that entered this turn, " +
        "create a tapped and attacking token that's a copy of that token. At the beginning of the " +
        "next end step, sacrifice those tokens."

    keywords(Keyword.FIRST_STRIKE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(
                (GameObjectFilter.Creature and GameObjectFilter.Token).youControl().enteredThisTurn()
            ),
            effect = Effects.CreateTokenCopyOfTarget(
                target = EffectTarget.IterationEntity,
                tapped = true,
                attacking = true,
                attackDefender = EnteringAttackDefender.CONTROLLER_CHOOSES,
                sacrificeAtStep = Step.END,
            )
        )
        description = "Whenever this creature attacks, for each creature token you control that entered " +
            "this turn, create a tapped and attacking token that's a copy of that token. At the " +
            "beginning of the next end step, sacrifice those tokens."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "37"
        artist = "Filipe Pagliuso"
        flavorText = "\"Blink or don't. You'll get hit either way.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fcddd8e1-8b8e-43b9-9922-6c3317cc45ae.jpg?1783907172"
        ruling("2025-04-04", "Although the tokens are attacking, they were never declared as attacking creatures. This means that abilities that trigger whenever a creature attacks won't trigger when these tokens enter attacking.")
        ruling("2025-04-04", "Any enters-the-battlefield abilities of the copied token will trigger when the token enters the battlefield. Any \"as [this creature] enters\" or \"[this creature] enters with\" abilities of the copied token will also work.")
        ruling("2025-04-04", "Each of the tokens created by Redoubled Stormsinger's last ability copies the original characteristics of the token it's copying as stated by the effect that created that token.")
        ruling("2025-04-04", "If a creature token you control that entered this turn is copying something else, then the token enters the battlefield as whatever that creature token copied.")
        ruling("2025-04-04", "You declare which player, planeswalker, or battle each token is attacking as you put it onto the battlefield. It doesn't have to be the same player, planeswalker, or battle as Redoubled Stormsinger or any of the other tokens created by Redoubled Stormsinger's last ability.")
    }
}
