package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Scarwood Bandits
 * {2}{G}{G}
 * Creature — Human Rogue
 * 2/2
 * Forestwalk
 * {2}{G}, {T}: Unless an opponent pays {2}, gain control of target artifact for as long as this
 * creature remains on the battlefield.
 *
 * A ransom rather than an outright theft: `PayOrSufferEffect` asks the opponent for {2} first and
 * only steals if they decline. The control effect is bounded by
 * [Duration.WhileSourceOnBattlefield], so killing the Bandits gives the artifact straight back —
 * which is what makes the {2} a real decision rather than a formality.
 *
 * The artifact is targeted when the ability is activated; the payment question comes at resolution,
 * so an opponent can also just remove the Bandits in response and never be asked.
 *
 * In multiplayer the printed "an opponent" means *any one* of them may pay; the payer here is
 * `Player.AnOpponent`, a single opponent, which is exact in a two-player game and narrower than the
 * card in a wider one — the any-player pay executor doesn't accept mana costs today.
 */
val ScarwoodBandits = card("Scarwood Bandits") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Rogue"
    power = 2
    toughness = 2
    oracleText = "Forestwalk (This creature can't be blocked as long as defending player controls " +
        "a Forest.)\n" +
        "{2}{G}, {T}: Unless an opponent pays {2}, gain control of target artifact for as long as " +
        "this creature remains on the battlefield."

    keywords(Keyword.FORESTWALK)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{G}"), Costs.Tap)
        target = Targets.Artifact
        effect = PayOrSufferEffect(
            cost = Costs.pay.Mana("{2}"),
            suffer = Effects.GainControl(
                EffectTarget.ContextTarget(0),
                Duration.WhileSourceOnBattlefield("this creature"),
            ),
            player = EffectTarget.PlayerRef(Player.AnOpponent),
            // The question goes to the victim, so it can't be phrased from the thief's side. The
            // generated text is `GainControlEffect`'s own words — "gain control of target for as
            // long as this creature remains on the battlefield" — which offers the opponent the
            // theft they are the subject of, and leaves both placeholders unresolved.
            consequenceDescription = "Scarwood Bandits' controller gains control of that artifact " +
                "for as long as Scarwood Bandits remains on the battlefield",
        )
        description = "{2}{G}, {T}: Unless an opponent pays {2}, gain control of target artifact " +
            "for as long as this creature remains on the battlefield."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "85"
        artist = "Mark Poole"
        imageUri = "https://cards.scryfall.io/normal/front/4/6/46b762a7-a774-4cb4-8ecf-dd6486a066c3.jpg?1783947930"
    }
}
