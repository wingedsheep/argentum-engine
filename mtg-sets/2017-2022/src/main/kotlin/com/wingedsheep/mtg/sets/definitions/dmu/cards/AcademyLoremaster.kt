package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Academy Loremaster
 * {U}{U}
 * Creature — Human Wizard
 * 2/3
 * At the beginning of each player's draw step, that player may draw an additional card. If they
 * do, spells they cast this turn cost {2} more to cast.
 *
 * The draw-step player ([Player.TriggeringPlayer]) makes the "may" choice, and the tax is
 * [Effects.IncreaseSpellCosts] scoped to that same player: it is fixed on the game state when the
 * trigger resolves, so it keeps applying even if Loremaster leaves the battlefield afterwards.
 */
val AcademyLoremaster = card("Academy Loremaster") {
    manaCost = "{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 3
    oracleText = "At the beginning of each player's draw step, that player may draw an additional card. " +
        "If they do, spells they cast this turn cost {2} more to cast."

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.DRAW)
        effect = Effects.May(
            Effects.DrawCards(1, EffectTarget.PlayerRef(Player.TriggeringPlayer)) then
                Effects.IncreaseSpellCosts(DynamicAmounts.fixed(2), player = Player.TriggeringPlayer),
            decisionMaker = EffectTarget.PlayerRef(Player.TriggeringPlayer),
            descriptionOverride = "Draw an additional card? Spells you cast this turn will cost {2} more.",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "40"
        artist = "Marcela Medeiros"
        flavorText = "As a young student, she dusted the shelves of Tolaria's great library. " +
            "Now, she writes the books that line them."
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9fca4c4f-a77b-483e-9da4-574ba2e3d179.jpg?1783921356"
    }
}
