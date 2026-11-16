package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Mirror Mockery
 * {1}{U}
 * Enchantment — Aura
 * Enchant creature
 * Whenever enchanted creature attacks, you may create a token that's a copy of that creature.
 * Exile that token at end of combat.
 *
 * Heat Shimmer's copy pointed at the attacking host: `Triggers.attached.attacks()` binds the
 * enchanted creature as the triggering entity, so "that creature" survives the Aura or the host
 * leaving before resolution — the copy then uses the creature's last-known copiable values
 * (CR 608.2h). The token is created under the Aura controller's control ("you"), not attacking,
 * and `exileAtStep = END_COMBAT` schedules a delayed exile for every token created (Doubling
 * Season included) that fires regardless of what happens to the Aura or the original.
 */
val MirrorMockery = card("Mirror Mockery") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Whenever enchanted creature attacks, you may create a token that's a copy of that creature. " +
        "Exile that token at end of combat."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.attached.attacks()
        effect = Effects.May(
            Effects.CreateTokenCopyOfTarget(
                target = EffectTarget.TriggeringEntity,
                exileAtStep = Step.END_COMBAT,
            ),
            prompt = "Create a token that's a copy of the enchanted creature?",
        )
        description = "Whenever enchanted creature attacks, you may create a token that's a copy of that " +
            "creature. Exile that token at end of combat."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "62"
        artist = "Ryan Alexander Lee"
        flavorText = "Who you are constantly runs toward who you will be."
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f837b050-70a6-46f0-963d-4badb37a42a1.jpg?1783938607"
        ruling("2015-02-25", "Notably, the token won't be attacking.")
        ruling(
            "2015-02-25",
            "If Mirror Mockery enchants a creature you don't control, and you are the defending player, " +
                "you can block with the token.",
        )
        ruling(
            "2015-02-25",
            "The ability that exiles the token will trigger at end of combat no matter what happens to the " +
                "original creature or to Mirror Mockery.",
        )
        ruling(
            "2015-02-25",
            "If Mirror Mockery is no longer on the battlefield as its triggered ability resolves, its ability " +
                "makes a copy of the creature it was enchanting when it left the battlefield. If it left the " +
                "battlefield because it was no longer enchanting a legal creature, it makes a copy of the " +
                "creature it was most recently enchanting before it left the battlefield.",
        )
        ruling(
            "2015-02-25",
            "If Mirror Mockery's ability creates multiple tokens (due to an effect such as the one Doubling " +
                "Season creates), all such tokens will be exiled by the delayed triggered ability.",
        )
    }
}
