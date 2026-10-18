package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Johan — {3}{R}{G}{W} Legendary Creature — Human Wizard, 5/4.
 *
 * Accepting the trigger gives Johan "can't attack" until end of combat, and sets up a rule
 * modification for the same combat: attacking doesn't cause creatures you control to tap — not
 * vigilance, so nothing gains an ability. Its "if Johan is untapped" is re-asked when attackers are
 * declared, and needs Johan still on the battlefield.
 */
val Johan = card("Johan") {
    manaCost = "{3}{R}{G}{W}"
    typeLine = "Legendary Creature — Human Wizard"
    power = 5
    toughness = 4
    oracleText = "At the beginning of combat on your turn, you may have Johan gain \"Johan can't attack\" " +
        "until end of combat. If you do, attacking doesn't cause creatures you control to tap this combat " +
        "if Johan is untapped."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        effect = Effects.May(
            Effects.CantAttack(EffectTarget.Self, Duration.EndOfCombat) then
                Effects.AttacksWithoutTappingGroup(
                    filter = GroupFilter.AllCreaturesYouControl,
                    duration = Duration.EndOfCombat,
                    condition = Conditions.SourceMatches(GameObjectFilter.Any.untapped().onBattlefield()),
                ),
            prompt = "Have Johan gain \"Johan can't attack\" until end of combat? If you do, attacking " +
                "doesn't cause creatures you control to tap this combat while Johan is untapped.",
        )
        description = "At the beginning of combat on your turn, you may have Johan gain \"Johan can't attack\" " +
            "until end of combat. If you do, attacking doesn't cause creatures you control to tap this combat " +
            "if Johan is untapped."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "236"
        artist = "Mark Tedin"
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b851d5b4-7991-49d4-8a52-bf233f960cbf.jpg?1783948038"
    }
}
