package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val NetherShadow = card("Nether Shadow") {
    manaCost = "{B}{B}"
    typeLine = "Creature — Spirit"
    oracleText = "Haste\nAt the beginning of your upkeep, if this card is in your graveyard with three or more creature cards above it, you may put this card onto the battlefield."
    power = 1
    toughness = 1
    keywords(Keyword.HASTE)
    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        triggerZone = Zone.GRAVEYARD
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.cardsAboveInGraveyard(filter = GameObjectFilter.Creature),
            ComparisonOperator.GTE, 3)
        effect = Effects.May(Effects.Move(EffectTarget.Self, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD),
            prompt = "Put Nether Shadow onto the battlefield?", sourceRequiredZone = Zone.GRAVEYARD)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "116"
        artist = "Christopher Rush"
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f13ad58a-6f9b-420a-bac1-40929f5e616a.jpg?1783948693"
        ruling("2008-10-01", "Players may not rearrange the cards in their graveyards.")
        ruling("2008-10-01", "The last thing that happens to a resolving instant or sorcery spell is that it's put into its owner's graveyard. Example: You cast Wrath of God. All creatures on the battlefield are destroyed. You arrange all the cards put into your graveyard this way in any order you want. The other players in the game do the same to the cards that are put into their graveyards. Then you put Wrath of God into your graveyard, on top of the other cards.")
        ruling("2008-10-01", "Say you're the owner of both a permanent and an Aura that's attached to it. If both the permanent and the Aura are destroyed at the same time (by Akroma's Vengeance, for example), you decide the order they're put into your graveyard. If just the enchanted permanent is destroyed, it's put into your graveyard first. Then, after state-based actions are checked, the Aura (which is no longer attached to anything) is put into your graveyard on top of it.")
        ruling("2008-10-01", "A card is \"above\" another card in your graveyard if it was put into that graveyard later.")
        ruling("2008-10-01", "If an effect or rule puts two or more cards into the same graveyard at the same time, the owner of those cards may arrange them in any order.")
        ruling("2004-10-04", "Since it enters due to triggering at the beginning of upkeep, it is not possible to get an infinite loop with four Nether Shadows.")
        ruling("2004-10-04", "Note that bringing the Shadow back onto the battlefield from the graveyard is not a spell, it is an ability. It can't be countered with something that counters spells.")
    }
}
