package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Whip of Erebos
 * {2}{B}{B}
 * Legendary Enchantment Artifact
 * Creatures you control have lifelink.
 * {2}{B}{B}, {T}: Return target creature card from your graveyard to the battlefield. It gains
 * haste. Exile it at the beginning of the next end step. If it would leave the battlefield, exile
 * it instead of putting it anywhere else. Activate only as a sorcery.
 *
 * The reanimation mirrors Wake to Slaughter / Kheru Lich Lord: the target is moved tracked, then
 * gets haste, a delayed "exile it at the next end step" trigger, and the exile-on-leave
 * replacement. Both exile effects live on the game, not on the creature (ruling), so the returned
 * creature losing its abilities doesn't stop them.
 */
val WhipOfErebos = card("Whip of Erebos") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Enchantment Artifact"
    oracleText = "Creatures you control have lifelink.\n" +
        "{2}{B}{B}, {T}: Return target creature card from your graveyard to the battlefield. It gains " +
        "haste. Exile it at the beginning of the next end step. If it would leave the battlefield, exile " +
        "it instead of putting it anywhere else. Activate only as a sorcery."

    staticAbility {
        ability = GrantKeyword(Keyword.LIFELINK, GroupFilter.AllCreaturesYouControl)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{B}{B}"), Costs.Tap)
        timing = TimingRule.SorcerySpeed
        target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.Pipeline {
            val chosen = gather(CardSource.ChosenTargets)
            val returned = moveTracked(chosen, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
            ifNotEmpty(returned) {
                // "It gains haste."
                run(Effects.GrantKeyword(Keyword.HASTE, returned.asTarget, Duration.Permanent))
                // "Exile it at the beginning of the next end step."
                run(Effects.CreateDelayedTrigger(step = Step.END, effect = Effects.Move(returned.asTarget, Zone.EXILE)))
                // "If it would leave the battlefield, exile it instead of putting it anywhere else."
                run(Effects.GrantExileOnLeave(returned.asTarget))
            }
        }
        description = "Return target creature card from your graveyard to the battlefield with haste; " +
            "exile it at the next end step or if it would leave the battlefield"
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "110"
        artist = "Yeong-Hao Han"
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e5ec47c-47a2-4eda-b81a-4a07f04e5989.jpg?1783939768"
        ruling(
            "2013-09-15",
            "At the beginning of the next end step, the creature returned to the battlefield with Whip of " +
                "Erebos is exiled. This is a delayed triggered ability. If the ability is countered, the " +
                "creature will stay on the battlefield and the delayed trigger won't trigger again. However, " +
                "the replacement effect will still exile the creature when it eventually leaves the battlefield.",
        )
        ruling(
            "2013-09-15",
            "Whip of Erebos grants haste to the creature that's returned to the battlefield. However, neither " +
                "of the \"exile\" abilities is granted to that creature. If that creature loses all its " +
                "abilities, it will still be exiled at the beginning of the end step, and if it would leave " +
                "the battlefield, it is still exiled instead.",
        )
        ruling(
            "2013-09-15",
            "The exiled creature is never put into the graveyard. Any abilities the creature has that " +
                "trigger when it dies won't trigger.",
        )
    }
}
