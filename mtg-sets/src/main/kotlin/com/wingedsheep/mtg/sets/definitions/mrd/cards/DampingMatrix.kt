package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities

/**
 * Damping Matrix — Mirrodin #161
 * {3} · Artifact
 *
 * Activated abilities of artifacts and creatures can't be activated unless they're mana abilities.
 *
 * The Cursed Totem shape widened by one axis: [PreventActivatedAbilities] over
 * `Artifact or Creature` with `nonManaAbilitiesOnly = true`, which is exactly the printed
 * "unless they're mana abilities" exemption. The union is homogeneous (both branches are bare
 * card-type predicates with no controller or state gate), so it collapses to a single
 * `CardPredicate.Or` — the flat shape the activation-legality check already understands, matched
 * against *projected* state so an animated Vehicle or a creature-land is covered while it is a
 * creature and not before.
 *
 * Everything outside the lock stays legal, per the 2017 rulings: triggered abilities, mana
 * abilities of the locked permanents, and activated abilities that function from other zones
 * (bloodrush, unearth, cycling) — the check only reads battlefield permanents.
 */
val DampingMatrix = card("Damping Matrix") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Activated abilities of artifacts and creatures can't be activated unless they're mana abilities."

    staticAbility {
        ability = PreventActivatedAbilities(
            filter = GameObjectFilter.Artifact or GameObjectFilter.Creature,
            nonManaAbilitiesOnly = true,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "161"
        artist = "Mike Dringenberg"
        flavorText = "The priests tried cursing it. The mages tried dispelling it. In the end, they all obeyed it."
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e2214e75-e469-41ae-8412-df725c097d08.jpg?1783944524"
        ruling(
            "2017-03-14",
            "Damping Matrix's ability affects only artifacts and creatures on the battlefield. Activated " +
                "abilities that work in other zones (such as bloodrush or unearth) can still be activated. " +
                "Triggered abilities (starting with \"when,\" \"whenever,\" or \"at\") are unaffected."
        )
        ruling("2017-03-14", "A mana ability is an ability that produces mana, not an ability that costs mana.")
    }
}
