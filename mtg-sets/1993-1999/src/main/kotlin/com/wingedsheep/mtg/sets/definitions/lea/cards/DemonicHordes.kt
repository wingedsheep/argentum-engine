package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Demonic Hordes
 * {3}{B}{B}{B}
 * Creature — Demon
 * 5/5
 * {T}: Destroy target land.
 * At the beginning of your upkeep, unless you pay {B}{B}{B}, tap this creature and sacrifice a
 * land of an opponent's choice.
 *
 * The upkeep tax is a resolution-time punisher ([Effects.PayOrSuffer]). Declining taps the Demon,
 * then an opponent picks one of *your* lands ([Chooser.Opponent] over a "lands you control"
 * gather) and you sacrifice it. Not targeted — the opponent's pick ignores shroud/hexproof. With no
 * lands, the tap still happens and the sacrifice does nothing.
 */
val DemonicHordes = card("Demonic Hordes") {
    manaCost = "{3}{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Demon"
    power = 5
    toughness = 5
    oracleText = "{T}: Destroy target land.\n" +
        "At the beginning of your upkeep, unless you pay {B}{B}{B}, tap this creature and " +
        "sacrifice a land of an opponent's choice."

    activatedAbility {
        cost = Costs.Tap
        val land = target(TargetFilter.Land)
        effect = Effects.Destroy(land)
        description = "{T}: Destroy target land."
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.PayOrSuffer(
            cost = Costs.pay.Mana(ManaCost.parse("{B}{B}{B}")),
            suffer = Effects.Tap(EffectTarget.Self) then Effects.Pipeline {
                val lands = gather(GameObjectFilter.Land, player = Player.You)
                val chosen = chooseExactly(
                    1,
                    from = lands,
                    chooser = Chooser.Opponent,
                    useTargetingUI = true,
                    prompt = "Choose a land for Demonic Hordes' controller to sacrifice",
                )
                sacrifice(chosen)
            },
        )
        description = "At the beginning of your upkeep, unless you pay {B}{B}{B}, tap this " +
            "creature and sacrifice a land of an opponent's choice."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "103"
        artist = "Jesper Myrfors"
        flavorText = "Created to destroy Dominia, Demons can sometimes be bent to a more focused purpose."
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6c9bb8b1-fb79-4b99-ba09-c6e6c860de50.jpg?1783948696"
    }
}
