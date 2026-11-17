package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.EnteringAttackDefender
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Flamerush Rider — Fate Reforged #99 (canonical printing)
 * {4}{R} · Creature — Human Warrior · 3/3
 *
 * Whenever this creature attacks, create a token that's a copy of another target attacking creature
 * and that's tapped and attacking. Exile the token at end of combat.
 * Dash {2}{R}{R}
 *
 * Thousand-Faced Shadow's copy shape on a `Triggers.self.attacks()` trigger, plus Mirror Mockery's
 * `exileAtStep = END_COMBAT`, which schedules the delayed exile for every token the effect creates
 * (Doubling Season included). The token's controller picks what it attacks as it enters
 * (CONTROLLER_CHOOSES), so it needn't attack the defender Flamerush Rider is attacking.
 */
val FlamerushRider = card("Flamerush Rider") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Warrior"
    power = 3
    toughness = 3
    oracleText = "Whenever this creature attacks, create a token that's a copy of another target attacking " +
        "creature and that's tapped and attacking. Exile the token at end of combat.\n" +
        "Dash {2}{R}{R} (You may cast this spell for its dash cost. If you do, it gains haste, and it's " +
        "returned from the battlefield to its owner's hand at the beginning of the next end step.)"

    dash = "{2}{R}{R}"

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val original = target(TargetFilter(GameObjectFilter.Creature.attacking()).other())
        effect = Effects.CreateTokenCopyOfTarget(
            target = original,
            tapped = true,
            attacking = true,
            attackDefender = EnteringAttackDefender.CONTROLLER_CHOOSES,
            exileAtStep = Step.END_COMBAT,
        )
        description = "Whenever this creature attacks, create a token that's a copy of another target " +
            "attacking creature and that's tapped and attacking. Exile the token at end of combat."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "99"
        artist = "Min Yum"
        imageUri = "https://cards.scryfall.io/normal/front/3/0/304107d6-d096-43a1-956f-5a3b6ed6dc60.jpg?1783938689"
        ruling("2014-11-24", "You choose which opponent or opposing planeswalker the token is attacking as you put it onto the battlefield. It doesn't have to be the same player or planeswalker Flamerush Rider is attacking.")
        ruling("2014-11-24", "Although the token is attacking, it was never declared as an attacking creature (for purposes of abilities that trigger whenever a creature attacks, for example).")
        ruling("2014-11-24", "The token copies exactly what was printed on the original creature and nothing else (unless that permanent is copying something else or is a token). It doesn't copy counters, Auras, Equipment, or non-copy effects. If the token isn't a creature as it enters the battlefield, it won't be attacking.")
        ruling("2014-11-24", "If another creature becomes or enters the battlefield as a copy of the token, you won't exile that creature at end of combat. However, if Flamerush Rider creates multiple tokens due to a replacement effect (like the one Doubling Season creates), you'll exile each of those tokens.")
    }
}
